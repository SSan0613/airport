package com.airport.airport.dto;

import lombok.Data;

@Data
public class TokenResponse {
    private String token;
    private long expiresIn;

    public TokenResponse(String token, long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}
