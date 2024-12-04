package com.airport.airport.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

@Getter
public class SignupRequest {
    @NotBlank(message="이름을 입력해 주세요")
    private String username;
    @Email(message = "이메일 형식이 맞지 않습니다")
    private String email;
    @NotBlank(message = "비밀번호를 입력해 주세요")
    @Size(min = 4 , message = "비밀번호는 4자 이상이어야 합니다")
    private String password;
    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String passwordconfirm;

    @AssertTrue(message = "비밀번호가 일치하지 않습니다")
    public boolean ispasswordMatching() {   //참이어야 유효하다고 판단함.
        return password != null && password.equals(passwordconfirm);
    }


}
