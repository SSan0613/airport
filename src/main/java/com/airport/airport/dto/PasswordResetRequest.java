package com.airport.airport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordResetRequest {
    @NotBlank(message = "이름을 입력해 주세요")
    private String username;
    @NotBlank(message = "이메일을 입력해 주세요")
    @Email(message = "이메일 형식이 맞지 않습니다")
    private String email;
}
