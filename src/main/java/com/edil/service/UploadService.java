package com.edil.service;

import com.edil.dto.UploadTicket;
import com.edil.dto.response.PresignResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UploadService {
    private final Cache<String, UploadTicket> uploadCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    public PresignResponse generatePresignedTicket(UUID creatorAccountId, String extension) {
        String ext = extension != null && extension.startsWith(".") ? extension : "." + (extension != null ? extension : "jpg");
        String fileId = UUID.randomUUID().toString() + ext;
        UploadTicket ticket = UploadTicket.builder()
                .creatorAccountId(creatorAccountId)
                .fileId(fileId)
                .confirmed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        uploadCache.put(fileId, ticket);
        return PresignResponse.builder()
                .fileId(fileId)
                .uploadUrl("http://localhost:8081/upload/" + fileId)
                .build();
    }

    public boolean validateTicketForNginxAuth(String fileId, UUID accountIdFromJwt) {
        UploadTicket ticket = uploadCache.getIfPresent(fileId);
        if (ticket == null) return false;
        return ticket.getCreatorAccountId().equals(accountIdFromJwt);
    }

    public void confirmUpload(String fileId, UUID creatorAccountId) {
        UploadTicket ticket = uploadCache.getIfPresent(fileId);
        if (ticket != null && ticket.getCreatorAccountId().equals(creatorAccountId)) {
            ticket.setConfirmed(true);
        }
    }

    public boolean isUploadConfirmed(String fileId) {
        UploadTicket ticket = uploadCache.getIfPresent(fileId);
        return ticket != null && ticket.isConfirmed();
    }

    public void invalidateTicket(String fileId) {
        uploadCache.invalidate(fileId);
    }
}
