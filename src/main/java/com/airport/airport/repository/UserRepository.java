package com.airport.airport.repository;

import com.airport.airport.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User,Long>{

    User findByEmail(String email);   //사용자 반환
    boolean existsByEmail(String email);  //사용자 중복확인
}
