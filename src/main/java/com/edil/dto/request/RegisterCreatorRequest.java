package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterCreatorRequest {
    @NotBlank
    private String email;
    @NotBlank
    @Size(min = 6)
    private String password;
    @NotBlank
    private String fullName;
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private String channelLink;
    
    private String aboutSection;
    
    @NotBlank
    private String payoutBankAccount;
}
