package com.airport.airport.controller;

import com.airport.airport.dto.LoginRequest;
import com.airport.airport.dto.TokenResponse;
import com.airport.airport.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    @Autowired
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 1. 인증 처리
            Authentication authentication = authService.authenticate(loginRequest.getLogin_id(), loginRequest.getPassword());
            log.info(String.valueOf(authentication));
            // 2. 인증 성공 시 토큰 생성

            log.info("토큰생성전이다");
            String token = authService.generateToken(loginRequest.getLogin_id());

            log.info(token);

            // 3. JWT 토큰 및 만료 시간 반환
            long expiresIn = 3600;
            TokenResponse tokenResponse = new TokenResponse(token, expiresIn);

            return ResponseEntity.ok(tokenResponse);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @GetMapping("/secure-resource")   //테스트용
    public String test() {
        return "This is a secure resource. Only authenticated users can see this.";
    }
}
