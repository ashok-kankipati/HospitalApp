package com.hospital.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max=100)
    private String username;
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max=72)
    @lombok.ToString.Exclude
    private String password;
}
