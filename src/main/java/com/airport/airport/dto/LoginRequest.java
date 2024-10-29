package com.airport.airport.dto;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class LoginRequest {

    private String email;
    private String password;
}
