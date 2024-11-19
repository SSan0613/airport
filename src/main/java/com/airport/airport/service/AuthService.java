package com.airport.airport.service;

import com.airport.airport.JwtUtil;
import com.airport.airport.domain.User;
import com.airport.airport.dto.PassRequest;
import com.airport.airport.dto.PasswordResetRequest;
import com.airport.airport.dto.SignupRequest;
import com.airport.airport.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Autowired
    public final UserRepository userRepository;
    @Autowired
    private final AuthenticationManager authenticationManager;  //기본적으로 설정된 authenticationProvider가 UserDetailsService에서 사용자 정보 로드.
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JavaMailSender javaMailSender;

/*
    //테스트용
    @PostConstruct
    private void test(){
        User test1 = new User("aa", passwordEncoder.encode("aa"),"example1@gmail.com");
        User test2 = new User("bb",passwordEncoder.encode("bb"),"example2@gmail.com");
        userRepository.save(test1);
        userRepository.save(test2);
    }
*/

    // 인증 메소드
    public Authentication authenticate(String email, String password) {
        try {

            // 인증 처리
            Authentication authentication = authenticationManager.authenticate( //실제 인증 수행
                    new UsernamePasswordAuthenticationToken(email, password) //인증 객체 생성
            );

            // 인증 성공 후 반환할 객체 로그 출력
            log.info("인증 성공: {}", authentication);

            return authentication; // 인증 결과 반환

        } catch (AuthenticationException e) {
            log.error("인증 실패 (AuthenticationException): {}", e.getMessage());       //추후에 아이디, 비밀번호 중 무엇이 오류인지 제공시에 수정일듯.
            throw e;    //인증 에러
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            throw new RuntimeException("Unexpected error occurred", e); // 예기치 않은 예외 처리
        }
    }

    // 토큰 생성 메소드
    public String generateToken(String email) {
        return jwtUtil.generateToken(email);
    }

    public User getUsername(String email){
        return userRepository.findByEmail(email);}

    //회원 가입 메소드
    public void registerUser(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일 형식입니다");       //이메일 이미 존재하는 경우. (여기서는 메시지만 반환 )
        }

        User user = new User(signupRequest.getUsername(), passwordEncoder.encode(signupRequest.getPassword()), signupRequest.getEmail()); //나중에 비밀번호 암호화 해야.. passwordEncoder;
        userRepository.save(user);

    }

    //비밀번호 변경 메소드
    public void updatePassword(PassRequest passRequest, String email) {
        User user = userRepository.findByEmail(email);

        if (!passwordEncoder.matches(passRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        user.changePassword(passRequest.getNewpassword(), passwordEncoder);
        userRepository.save(user);
    }

    //임시 비밀번호 발급
    public void temporaryPassword(PasswordResetRequest passwordResetRequest) {
        String email = passwordResetRequest.getEmail();
        String username = passwordResetRequest.getUsername();
        if (!userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("존재하지 않는 이메일입니다");
        }
        User user = userRepository.findByEmail(email);
        if (!user.getUsername().equals(username)) {
            throw new IllegalArgumentException("이름이 일치하지 않습니다");
        }
        String temp_password = generateTemporaryPassword();

        //임시 비밀번호로 저장
        user.changePassword(temp_password, passwordEncoder);
        userRepository.save(user);

        //이메일 전송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("임시 비밀번호 입니다");
        message.setText(username+"님의 임시 비밀번호입니다: " + temp_password + "\n로그인 후 비밀번호를 반드시 변경해 주세요.");
        message.setFrom("skyroute1112@gmail.com"); // 보내는 사람 설정
        javaMailSender.send(message);

    }
    private String generateTemporaryPassword() {
        int length = 8;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder tempPassword = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            tempPassword.append(chars.charAt(index));
        }
        return tempPassword.toString();
    }

}
