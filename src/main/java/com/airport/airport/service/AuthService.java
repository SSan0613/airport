package com.airport.airport.service;

import com.airport.airport.JwtUtil;
import com.airport.airport.domain.User;
import com.airport.airport.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Autowired
    public final UserRepository userRepository;
    @Autowired
    private final AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;

    @PostConstruct
    private void test(){                //테스트용
        User test1 = new User("aa", "aa");
        User test2 = new User("bb","bb");
        userRepository.save(test1);
        userRepository.save(test2);
    }

    // 인증 메소드
    public Authentication authenticate(String username, String password) {
        try {
            // 인증 처리 전 로그 출력
            log.info("인증처리 authenticate 들어가기 전입니다");

            // 인증 처리
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // 인증 성공 후 반환할 객체 로그 출력
            log.info("인증 성공: {}", authentication);

            return authentication; // 인증 결과 반환

        } catch (AuthenticationException e) {
            log.error("인증 실패 (AuthenticationException): {}", e.getMessage(), e); // 스택 트레이스 추가
            throw new RuntimeException("Invalid username or password", e);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);  // 예기치 않은 예외 처리
            throw new RuntimeException("Unexpected error occurred", e);
        } finally {
            log.info("인증처리 했습니다.");
        }
    }

    // 토큰 생성 메소드
    public String generateToken(String username) {
        return jwtUtil.generateToken(username);
    }
}
