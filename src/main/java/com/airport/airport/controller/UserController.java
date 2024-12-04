package com.airport.airport.controller;

import com.airport.airport.domain.User;
import com.airport.airport.dto.*;
import com.airport.airport.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            //인증 처리
            Authentication authentication = authService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());

            // 인증 성공 시 토큰 생성
            String token = authService.generateToken(loginRequest.getEmail());

            //JWT 토큰 및 만료 시간 반환
            long expiresIn = 3600;
            User user = authService.getUsername(loginRequest.getEmail());
            AuthResponse tokenResponse = new AuthResponse(token, user.getUsername(), expiresIn);

            return ResponseEntity.ok(tokenResponse);        //성공시 200. 토큰과 만료시간 반환.
        }
        catch (AuthenticationException e) { //자격 증명 실패
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)   // 로그인 실패는 401 반환.
                    .body(Map.of( "message", "이메일이나 비밀번호 오류"));
        }
        catch (Exception e) {   //그 외의 오류
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)      //서버 내 오류 500 반환.
                    .body(Map.of("message","서버 오류"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest, BindingResult bindingResult) {
        // ( signupRequest에서 예외 발생 시
        ResponseEntity<?> checkErrorRequest = validateRequest(bindingResult);
        if(checkErrorRequest!=null) return checkErrorRequest;

        try {
            authService.registerUser(signupRequest);
            return ResponseEntity.ok(Map.of("message", "회원가입 성공")); //가입 성공
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(   //이미 존재하는 사용자 예외 처리(에러 메시지 그대로 400 반환.)
                    "message", e.getMessage()));
        }  catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of( //예기치 못한 나머지 예외에 대한 처리. (500)
                    "message", "서버 오류."));
        }
    }

    /*  @PostMapping("/logout")
      public ResponseEntity<?> logout() {

      }*/
    @PostMapping("/update")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PassRequest passRequest, BindingResult bindingResult) {

        ResponseEntity<?> checkErrorRequest = validateRequest(bindingResult);
        if(checkErrorRequest!=null) return checkErrorRequest;           //나중에 예외 처리 귀찮다

        try {
            String useremail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            authService.updatePassword(passRequest,useremail);
            return ResponseEntity.ok(Map.of("messsage","비밀번호 변경 성공"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage()));
        }
    }

    @PostMapping("/findpassword")
    public ResponseEntity<?> findPassword(@Valid @RequestBody PasswordResetRequest passwordResetRequest, BindingResult bindingResult) {
        ResponseEntity<?> checkErrorRequest = validateRequest(bindingResult);
        if(checkErrorRequest!=null) return checkErrorRequest;
        try {
            authService.temporaryPassword(passwordResetRequest);
            return ResponseEntity.ok(Map.of("message","임시 비밀번호가 메일로 전송되었습니다."));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }

    }
    @GetMapping("/secure-resource")   //테스트용
    public String test() {

        return "This is a secure resource. Only authenticated users can see this."; //
    }

    private ResponseEntity<?> validateRequest(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();

            for (FieldError fieldError : bindingResult.getFieldErrors()) {  //@valid에서 터진 예외 전부 처리(비밀번호 숫자, 공백, 이메일 형식 등) (400)
                String defaultMessage = fieldError.getDefaultMessage();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message",defaultMessage));
            }
        }
        return null;
    }
}
