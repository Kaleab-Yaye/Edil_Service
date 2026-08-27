package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterUserRequest {
    @NotBlank
    private String email;
    @NotBlank
    @Size(min = 6)
    private String password;
    @NotBlank
    private String fullName;
    @NotBlank
    private String phoneNumber;
    
    private String address;
    
    @NotBlank
    private String refundBankAccount;
}
