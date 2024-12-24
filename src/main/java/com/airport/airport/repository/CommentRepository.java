package com.airport.airport.repository;

import com.airport.airport.domain.Comment;
import com.airport.airport.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findByRoute_RouteIdOrderByUpdatedTimeDesc(String routeId);

  boolean existsByCommentId(Long commentId);  //사용자 중복확인

  Comment findByCommentId(Long commentId);

  Page<Comment> findAllByUser(User user, Pageable pageable);

}