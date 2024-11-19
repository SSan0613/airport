package com.airport.airport.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String jwt;
    private String username;
    private long expiresIn;

    public AuthResponse(String jwt,String username, long expiresIn) {
        this.jwt = jwt;
        this.username=username;
        this.expiresIn = expiresIn;
    }
}
