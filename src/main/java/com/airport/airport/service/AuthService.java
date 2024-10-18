package com.airport.airport.service;

import com.airport.airport.JwtUtil;
import com.airport.airport.domain.User;
import com.airport.airport.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Autowired
    public final UserRepository userRepository;
    @Autowired
    private final AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;

    @PostConstruct
    private void test(){
        User test1 = new User("aa", "aa");
        userRepository.save(test1);
    }

    // 인증 메소드
    public Authentication authenticate(String username, String password) {
        try {
            // 인증 처리
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid username or password");
        }
    }

    // 토큰 생성 메소드
    public String generateToken(String username) {
        return jwtUtil.generateToken(username);
    }
}
