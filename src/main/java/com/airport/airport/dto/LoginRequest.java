package com.airport.airport.dto;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class LoginRequest {

    private String login_id;
    private String password;
}