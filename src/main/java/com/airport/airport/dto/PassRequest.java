package com.airport.airport.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PassRequest {
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 4, message = "비밀번호는 4자 이상이어야 합니다")
    private String newpassword;
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 4, message = "비밀번호는 4자 이상이어야 합니다")
    private String newpasswordconfirm;

    @AssertTrue(message = "새 비밀번호가 일치하지 않습니다")
    public boolean ispasswordMatching() {
        return newpassword != null && newpassword.equals(newpasswordconfirm);
    }

    @AssertTrue(message = "현재 비밀번호와 새 비밀번호가 일치합니다")
    public boolean ispasswordDiferent() {
        return password != null && !password.equals(newpassword);
    }
}
