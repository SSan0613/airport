package com.airport.airport.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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

    public User() {

    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password=password;
        this.email = email;
    }
}
