package com.edil.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMeResponse {
    private UUID accountId;
    private String email;
    private String role;
    private boolean active;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String refundBankAccount;
    private String channelLink;
    private String aboutSection;
    private String payoutBankAccount;
    private String onboardingStatus;
}
