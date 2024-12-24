package com.airport.airport.service;

import com.airport.airport.domain.Comment;
import com.airport.airport.domain.Route;
import com.airport.airport.domain.User;
import com.airport.airport.domain.Vote;
import com.airport.airport.dto.CommentDeleteRequest;
import com.airport.airport.dto.CommentRequest;
import com.airport.airport.dto.RouteCommentsResponse;
import com.airport.airport.dto.UserCommentResponse;
import com.airport.airport.repository.CommentRepository;
import com.airport.airport.repository.RouteRepository;
import com.airport.airport.repository.UserRepository;
import com.airport.airport.repository.VoteRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    @Autowired
    private final CommentRepository commentRepository;
    @Autowired
    private final VoteRepository voteRepository;
    @Autowired
    private final RouteRepository routeRepository;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;
  /*  //테스트용
    @PostConstruct
    private void test(){
        Route route1 = new Route(1L, "군자동");
        Route route2 = new Route(2L, "화양동");

        routeRepository.save(route1);
        routeRepository.save(route2);

        User user = new User("aa", passwordEncoder.encode("aa"),"example3@gmail.com");
        userRepository.save(user);

        Comment comment1 = new Comment(route1,user,"경로가 좋아요");
        Comment comment2 = new Comment(route1,user,"생각해보니 별로 안좋아요");
        Comment comment3 = new Comment(route1,user,"comment3 별로 안좋아요");
        Comment comment4 = new Comment(route1,user,"comment4 별로 안좋아요");
        Comment comment5= new Comment(route1,user,"comment5 별로 안좋아요");
        Comment comment6 = new Comment(route1,user,"comment6 별로 안좋아요");
        Comment comment7 = new Comment(route1,user,"comment7 별로 안좋아요");
        Comment comment8 = new Comment(route1,user,"comment8 별로 안좋아요");
        Comment comment9 = new Comment(route1,user,"comment9 별로 안좋아요");
        Comment comment10 = new Comment(route1,user,"comment10 별로 안좋아요");
        Comment comment11 = new Comment(route1,user,"comment11 별로 안좋아요");

        Comment comment12 = new Comment(route2,user,"경로가 안좋아요");
        Comment comment13 = new Comment(route2,user,"진짜 안좋아요");


        commentRepository.save(comment1);
        commentRepository.save(comment2);
        commentRepository.save(comment3);
        commentRepository.save(comment4);
        commentRepository.save(comment4);
        commentRepository.save(comment5);
        commentRepository.save(comment6);
        commentRepository.save(comment7);
        commentRepository.save(comment8);
        commentRepository.save(comment9);
        commentRepository.save(comment10);
        commentRepository.save(comment11);
        commentRepository.save(comment12);
        commentRepository.save(comment13);



        Vote vote1 = new Vote(1,15,route1,user);
        voteRepository.save(vote1);
    }
*/

    public RouteCommentsResponse getCommentsByRouteId(String route_id, String useremail) throws IllegalAccessException {
        if (!routeRepository.existsByRouteId(route_id)) {
            throw new IllegalAccessException("존재하지 않는 경로입니다");
        }
        Route route = routeRepository.findByRouteId(route_id);
        log.info(String.valueOf(route_id));
        List<RouteCommentsResponse.CommentDetail> comments = commentRepository.findByRoute_RouteIdOrderByUpdatedTimeDesc(route_id).stream()
                .map(comment -> new RouteCommentsResponse.CommentDetail(
                        comment.getCommentId(),
                        comment.getContent(),
                        comment.getUser().getUsername(),
                        comment.getUser().getEmail(),
                        comment.getUpdatedTime()
                ))
                .collect(Collectors.toList());

        int positive;
        int negative;
        Optional<Integer> positiveByRouteId = voteRepository.findPositiveByRoute_id(route_id);
        Optional<Integer> negativeByRouteId = voteRepository.findNegativeByRoute_id(route_id);

      if(positiveByRouteId.isEmpty()){
          positive=0;
          negative=0;
      }
      else{
          positive=positiveByRouteId.get();
          negative = negativeByRouteId.get();
      }
        User user = userRepository.findByEmail(useremail);
        Optional<Vote> vote = voteRepository.findByUserAndRoute(user, route);
        String like_status = "none";
        if (vote.isEmpty() || (vote.get().getNegative()==0 && vote.get().getPositive()==0)) {
            like_status="none";
        } else if (vote.get().getPositive()>0) {
            like_status="positive";
        }else if (vote.get().getNegative()>0){
            like_status="negative";
        }
        return new RouteCommentsResponse(positive, negative,like_status,comments);
    }

    public void addComment(String routeId, String useremail, CommentRequest commentRequest) {
        Route route = routeRepository.findByRouteId(routeId);  //예외처리 필요
        User user = userRepository.findByEmail(useremail);      //예외처리 필요

        Comment comment = new Comment(route,user,commentRequest.getContent());
        commentRepository.save(comment);
    }

    public void editComment(Long commentId, String newComment, String useremail) throws AuthenticationException {
        if (!commentRepository.existsByCommentId(commentId)) {
            throw new IllegalArgumentException("잘못된 접근(comment_id 불일치)");
        }

        Comment comment = commentRepository.findByCommentId(commentId);
        if(!comment.getUser().getEmail().equals(useremail)){
            throw new AuthenticationException("수정 권한이 없습니다");
        }
        comment.updateContent(newComment);
        commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId,String useremail) throws AuthenticationException {
        if (!commentRepository.existsByCommentId(commentId)) {
            throw new IllegalArgumentException("잘못된 접근(comment_id 불일치)");
        }
        Comment comment = commentRepository.findByCommentId(commentId);
        if(!comment.getUser().getEmail().equals(useremail)){
            throw new AuthenticationException("삭제 권한이 없습니다");
        }
        commentRepository.deleteById(commentId);
    }

    public Map<String,Object> recommendRoute(String routeId, String useremail) {
        if (!routeRepository.existsByRouteId(routeId)) {
            throw new IllegalArgumentException("잘못된 접근(route_id) 존재 x");            //나중에 최적화 하기
        }
        Route route = routeRepository.findByRouteId(routeId);
        if (!userRepository.existsByEmail(useremail)) {
            throw  new IllegalArgumentException("로그인을 다시 시도해 주세요");              //나중에 최적화 하기
        }
        User user = userRepository.findByEmail(useremail);

        String like_status = "none";
        if (voteRepository.findByUserAndRoute(user, route).isPresent()) {
            Vote vote = voteRepository.findByUserAndRoute(user, route).get();
            if(vote.getNegative()>0){
                vote.increasePositive();
                vote.decreaseNegative();
                like_status = "positive";
            }
            else if(vote.getPositive()>0){      //긍정 부정 둘다 0이면 테이블 삭제해야하나?
                vote.decreasePositive();
            }
            else{
                vote.increasePositive();
                like_status = "positive";
            }
            voteRepository.save(vote);
        }
        else{
            Vote vote = new Vote(0, 1, route, user);
            like_status="positive";
            voteRepository.save(vote);
        }


        Map<String, Object> response = new HashMap<>();
        response.put("positive", voteRepository.findPositiveByRoute_id(route.getRouteId()));
        response.put("negative", voteRepository.findNegativeByRoute_id(route.getRouteId()));
        response.put("like_status",like_status);
        return response;
    }

    public Map<String,Object> notRecommendRoute(String routeId, String useremail) {
        if (!routeRepository.existsByRouteId(routeId)) {
            throw new IllegalArgumentException("잘못된 접근(route_id) 존재 x");            //나중에 최적화 하기
        }
        Route route = routeRepository.findByRouteId(routeId);
        if (!userRepository.existsByEmail(useremail)) {
            throw  new IllegalArgumentException("로그인을 다시 시도해 주세요");              //나중에 최적화 하기
        }

        User user = userRepository.findByEmail(useremail);
        String like_status = "none";
        if (voteRepository.findByUserAndRoute(user, route).isPresent()) {
            Vote vote = voteRepository.findByUserAndRoute(user, route).get();
            if(vote.getPositive()>0){
                vote.decreasePositive();
                vote.increaseNegative();
                like_status = "negative";
            }
            else if(vote.getNegative()>0){  //긍정 부정 둘다 0이면 테이블 삭제해야하나?
                vote.decreaseNegative();
            }
            else{
                vote.increaseNegative();
                like_status = "negative";
            }
            voteRepository.save(vote);
        }
        else{
            Vote vote = new Vote(1, 0, route, user);
            like_status="negative";
            voteRepository.save(vote);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("positive", voteRepository.findPositiveByRoute_id(route.getRouteId()));
        response.put("negative", voteRepository.findNegativeByRoute_id(route.getRouteId()));
        response.put("like_status",like_status);
        return response;
    }

    public Page<UserCommentResponse> getCommentsByUser(String useremail, int page) {
        User user = userRepository.findByEmail(useremail);      //나ㅏ중에 예외

        Pageable pageable = PageRequest.of(page, 5,Sort.by("updatedTime").descending());
        Page<Comment> pageComment = commentRepository.findAllByUser(user, pageable);
        Page<UserCommentResponse> userCommentResponses = pageComment.map(
                comment -> new UserCommentResponse(
                        comment.getCommentId(),
                        comment.getContent(),
                        comment.getUpdatedTime(),
                        comment.getRoute().getName()
                ));

        return userCommentResponses;
    }

    public void deleteComments(CommentDeleteRequest commentDeleteRequest, String useremail) throws AuthenticationException {
        List<Long> commentIdList = commentDeleteRequest.getCommentIdList();
        log.info(commentIdList.toString());
        List<Comment> comments = commentRepository.findAllById(commentIdList);
        for (Comment comment : comments) {
            if (!comment.getUser().getEmail().equals(useremail)) {
                throw new AuthenticationException("삭제 권한이 없습니다");
            }
        }

        commentRepository.deleteAllById(commentIdList);
    }
}
