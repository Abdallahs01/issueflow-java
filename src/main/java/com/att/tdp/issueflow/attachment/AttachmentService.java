package com.att.tdp.issueflow.attachment;

import com.att.tdp.issueflow.attachment.dto.AttachmentResponse;
import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.common.ResourceNotFoundException;
import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class AttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "text/plain"
    );

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public AttachmentService(AttachmentRepository attachmentRepository, TicketRepository ticketRepository, AuditLogService auditLogService) {
        this.attachmentRepository = attachmentRepository;
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) {
        Ticket ticket = findActiveTicketById(ticketId);
        validateFile(file);

        try {
            Attachment attachment = new Attachment(
                    ticket,
                    safeFileName(file),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );

            Attachment savedAttachment = attachmentRepository.save(attachment);
            auditLogService.recordCurrentUserAction("ATTACHMENT", savedAttachment.getId(), "CREATE", "Attachment was added to ticket " + ticket.getId() + ".");

            return AttachmentResponse.from(savedAttachment);
        } catch (IOException exception) {
            throw new BadRequestException("Could not read uploaded file.");
        }
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long ticketId) {
        findActiveTicketById(ticketId);

        return attachmentRepository.findByTicketId(ticketId)
                .stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional
    public void deleteAttachment(Long ticketId, Long attachmentId) {
        findActiveTicketById(ticketId);
        Attachment attachment = attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment was not found."));

        attachmentRepository.delete(attachment);
        auditLogService.recordCurrentUserAction("ATTACHMENT", attachment.getId(), "DELETE", "Attachment was deleted from ticket " + ticketId + ".");
    }

    private Ticket findActiveTicketById(Long ticketId) {
        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket was not found."));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attachment file is required.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Attachment file size cannot exceed 10 MB.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Attachment content type is not allowed.");
        }
    }

    private String safeFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            return "attachment";
        }

        return originalFileName.replace("\\", "_").replace("/", "_");
    }
}
