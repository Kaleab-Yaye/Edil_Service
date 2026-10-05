package com.edil.service;

import com.edil.dto.response.PresignResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UploadServiceTest {

    private UploadService uploadService;
    private UUID creatorAccountId;

    @BeforeEach
    void setUp() {
        uploadService = new UploadService();
        creatorAccountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("generatePresignedTicket should create ticket with default .jpg when extension is null")
    void shouldCreateTicketWithDefaultExtension() {
        PresignResponse response = uploadService.generatePresignedTicket(creatorAccountId, null);

        assertThat(response).isNotNull();
        assertThat(response.getFileId()).endsWith(".jpg");
        assertThat(response.getUploadUrl()).startsWith("http://localhost:8081/upload/");
        assertThat(uploadService.isUploadConfirmed(response.getFileId())).isFalse();
    }

    @Test
    @DisplayName("generatePresignedTicket should preserve custom extensions")
    void shouldPreserveCustomExtensions() {
        PresignResponse pngResponse = uploadService.generatePresignedTicket(creatorAccountId, ".png");
        assertThat(pngResponse.getFileId()).endsWith(".png");

        PresignResponse webpResponse = uploadService.generatePresignedTicket(creatorAccountId, "webp");
        assertThat(webpResponse.getFileId()).endsWith(".webp");
    }

    @Test
    @DisplayName("validateTicketForNginxAuth should return true when account matches and false otherwise")
    void shouldValidateTicketForNginxAuth() {
        PresignResponse response = uploadService.generatePresignedTicket(creatorAccountId, ".jpg");

        // Matching account
        boolean isValid = uploadService.validateTicketForNginxAuth(response.getFileId(), creatorAccountId);
        assertThat(isValid).isTrue();

        // Mismatched account
        UUID anotherAccount = UUID.randomUUID();
        boolean isInvalid = uploadService.validateTicketForNginxAuth(response.getFileId(), anotherAccount);
        assertThat(isInvalid).isFalse();

        // Non-existent file
        assertThat(uploadService.validateTicketForNginxAuth("non-existent-file.jpg", creatorAccountId)).isFalse();
    }

    @Test
    @DisplayName("confirmUpload should confirm upload only when creatorAccountId matches")
    void shouldConfirmUpload() {
        PresignResponse response = uploadService.generatePresignedTicket(creatorAccountId, ".jpg");
        String fileId = response.getFileId();

        assertThat(uploadService.isUploadConfirmed(fileId)).isFalse();

        // Attempt confirm with wrong account -> remains false
        uploadService.confirmUpload(fileId, UUID.randomUUID());
        assertThat(uploadService.isUploadConfirmed(fileId)).isFalse();

        // Confirm with correct account -> becomes true
        uploadService.confirmUpload(fileId, creatorAccountId);
        assertThat(uploadService.isUploadConfirmed(fileId)).isTrue();
    }

    @Test
    @DisplayName("invalidateTicket should remove ticket from cache")
    void shouldInvalidateTicket() {
        PresignResponse response = uploadService.generatePresignedTicket(creatorAccountId, ".jpg");
        String fileId = response.getFileId();

        uploadService.confirmUpload(fileId, creatorAccountId);
        assertThat(uploadService.isUploadConfirmed(fileId)).isTrue();

        uploadService.invalidateTicket(fileId);
        assertThat(uploadService.isUploadConfirmed(fileId)).isFalse();
        assertThat(uploadService.validateTicketForNginxAuth(fileId, creatorAccountId)).isFalse();
    }
}
