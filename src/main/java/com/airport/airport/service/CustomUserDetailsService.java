package com.airport.airport.service;

import com.airport.airport.CustomUserDetails;
import com.airport.airport.domain.User;
import com.airport.airport.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        // 사용자가 존재하지 않으면 예외 발생
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        // 사용자가 존재하면 UserDetails 객체로 반환
        return new CustomUserDetails(user);
    }

}
