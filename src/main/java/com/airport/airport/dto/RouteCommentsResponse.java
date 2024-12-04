package com.airport.airport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class RouteCommentsResponse {

    private int positive;
    private int negative;
    private String like_status;

    private List<CommentDetail> comments;



    @Data
    public static class CommentDetail{
        private Long comment_id;
        private String content;
        private String username;
        private LocalDateTime updated_time;
        private String email;

        public CommentDetail(Long comment_id, String content, String username, String email, LocalDateTime updated_time) {
            this.comment_id = comment_id;
            this.content = content;
            this.username = username;
            this.email = email;
            this.updated_time = updated_time;
        }
    }
}
