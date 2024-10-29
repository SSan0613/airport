package com.airport.airport.dto;

import lombok.Data;

@Data
public class TokenResponse {
    private String jwt;
    private long expiresIn;

    public TokenResponse(String jwt, long expiresIn) {
        this.jwt = jwt;
        this.expiresIn = expiresIn;
    }
}
