package com.att.tdp.issueflow.attachment.dto;

import com.att.tdp.issueflow.attachment.Attachment;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        Long ticketId,
        String fileName,
        String contentType,
        long sizeBytes,
        Instant createdAt
) {

    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getTicket().getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getCreatedAt()
        );
    }
}
