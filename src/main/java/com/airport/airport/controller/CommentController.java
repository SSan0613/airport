package com.airport.airport.controller;

import com.airport.airport.JwtUtil;
import com.airport.airport.domain.Comment;
import com.airport.airport.domain.User;
import com.airport.airport.dto.CommentDeleteRequest;
import com.airport.airport.dto.CommentRequest;
import com.airport.airport.dto.RouteCommentsResponse;
import com.airport.airport.dto.UserCommentResponse;
import com.airport.airport.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private final CommentService commentService;
    @Autowired
    private final JwtUtil jwtUtil;

    //의견 달기 클릭 시 해당 경로의 모든 댓글 조회
    @GetMapping("/{route_id}")
    public ResponseEntity<?> getRouteComments(@PathVariable("route_id") String route_id) {
        String useremail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info(String.valueOf(route_id));
        try {
            RouteCommentsResponse commentsByRouteId = commentService.getCommentsByRouteId(route_id, useremail);
            return ResponseEntity.status(HttpStatus.OK).body(commentsByRouteId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    //경로에 의견 달기
    @PostMapping("/{route_id}")
    public ResponseEntity<?> postComment(@PathVariable("route_id") String routeId, @Valid @RequestBody CommentRequest commentRequest,BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) {
            String errorMessage = bindingResult.getFieldErrors().get(0).getDefaultMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", errorMessage));
        }
        try {
            String useremail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            commentService.addComment(routeId, useremail, commentRequest);
            return ResponseEntity.ok(Map.of("messsage", "댓글 작성 완료!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage()));
        }
    }

    //사용자 댓글 수정
    @PutMapping("/{comment_id}")
    public ResponseEntity<?> editComment(@PathVariable("comment_id") Long commentId, @Valid @RequestBody CommentRequest commentRequest, BindingResult bindingResult) {
        // ( signupRequest에서 예외 발생 시
        if (bindingResult.hasFieldErrors()) {
            String errorMessage = bindingResult.getFieldErrors().get(0).getDefaultMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", errorMessage));
        }
        String newComment = commentRequest.getContent();
        String useremail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            commentService.editComment(commentId, newComment,useremail);
            return ResponseEntity.ok(Map.of("messsage", "댓글 수정 완료!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    //사용자 댓글 삭제
    @DeleteMapping({"/{comment_id}", "/user/{comment_id}"})
    public ResponseEntity<?> deleteComment(@PathVariable("comment_id") Long commentId) {
        String useremail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            commentService.deleteComment(commentId,useremail);
            return ResponseEntity.ok(Map.of("messsage", "댓글 삭제 완료!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    //경로에 추천
    @GetMapping("/{route_id}/recommend")
    public ResponseEntity<Map<String, Object>> recommend(@PathVariable("route_id") String routeId) {
        try {
            String useremail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Map<String, Object> response = commentService.recommendRoute(routeId, useremail);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    //경로에 비추천
    @GetMapping("/{route_id}/notrecommend")
    public ResponseEntity<Map<String, Object>> notrecommend(@PathVariable("route_id") String routeId) {
        try {
            String useremail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Map<String, Object> response = commentService.notRecommendRoute(routeId, useremail);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    //사용자가 작성한 댓글 조회
    @GetMapping("/user")
    public ResponseEntity<?> getUserComments(@RequestParam("page") int page) {
        String useremail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Page<UserCommentResponse> comments = commentService.getCommentsByUser(useremail, page);

        if((page >= comments.getTotalPages() && page>0) || page<0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message","존재하지 않는 페이지입니다"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", comments.getContent());
        response.put("pageNumber", comments.getNumber());
        response.put("totalPages", comments.getTotalPages());
        response.put("isLast", comments.isLast());

        return ResponseEntity.ok(response);
    }

    //댓글 여러개 삭제
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteComments(@RequestBody @Valid CommentDeleteRequest commentDeleteRequest, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) {
            String errorMessage = bindingResult.getFieldErrors().get(0).getDefaultMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", errorMessage));
        }
        try {
            String useremail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            commentService.deleteComments(commentDeleteRequest, useremail);
            return ResponseEntity.ok((Map.of("messsage", "댓글 삭제 완료!")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
}


