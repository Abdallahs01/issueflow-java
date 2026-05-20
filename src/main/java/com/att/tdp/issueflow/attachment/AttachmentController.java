package com.att.tdp.issueflow.attachment;

import com.att.tdp.issueflow.attachment.dto.AttachmentResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping
    public AttachmentResponse uploadAttachment(@PathVariable Long ticketId, @RequestParam("file") MultipartFile file) {
        return attachmentService.uploadAttachment(ticketId, file);
    }

    @GetMapping
    public List<AttachmentResponse> getAttachments(@PathVariable Long ticketId) {
        return attachmentService.getAttachments(ticketId);
    }

    @DeleteMapping("/{attachmentId}")
    public void deleteAttachment(@PathVariable Long ticketId, @PathVariable Long attachmentId) {
        attachmentService.deleteAttachment(ticketId, attachmentId);
    }
}
