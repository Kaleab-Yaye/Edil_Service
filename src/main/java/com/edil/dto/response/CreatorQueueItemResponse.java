package com.edil.dto.response;

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
public class CreatorQueueItemResponse {
    private UUID accountId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String channelLink;
    private String aboutSection;
    private String payoutBankAccount;
    private String onboardingStatus;
    private LocalDateTime submittedAt;
}
