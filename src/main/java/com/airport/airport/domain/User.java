package com.airport.airport.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@RequiredArgsConstructor
@Getter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long user_id;

    @Column(name = "login_id")
    private String loginId;
    @Column
    private String username;
    @Column
    private String password;
    @Column
    private String email;


    public User(String loginId, String password) {
        this.loginId = loginId;
        this.password=password;
    }
}
