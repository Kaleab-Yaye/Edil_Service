package com.edil.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadTicket {
    private UUID creatorAccountId;
    private String fileId;
    private boolean confirmed;
    private LocalDateTime expiresAt;
}
