package com.edil.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private UUID accountId;
    private String email;
    private String fullName;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;
}
