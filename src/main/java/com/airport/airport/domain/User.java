package com.airport.airport.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Entity
@RequiredArgsConstructor
@Getter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long user_id;

/*    @Column(name = "login_id")
    private String loginId;*/
    @Column
    @NonNull
    private String username;
    @Column
    @NonNull
    private String password;
    @Column
    private String email;

    @OneToMany(mappedBy = "user")
    private List<Comment> comments = new ArrayList<>();

    public User() {

    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password=password;
        this.email = email;
    }

    public void changePassword(String newpassword, PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(newpassword);
    }
}
