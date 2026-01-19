package com.documentsearch.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private String tenantId;
    private String role;
    private Long expiresIn;

    public AuthResponse(String token, String username, String tenantId, String role, Long expiresIn) {
        this.token = token;
        this.username = username;
        this.tenantId = tenantId;
        this.role = role;
        this.expiresIn = expiresIn;
    }
}

