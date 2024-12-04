package com.airport.airport.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class UserCommentResponse {
    private Long comment_id;
    private String content;
    private LocalDateTime updated_time;
    private String routeName;

    public UserCommentResponse(Long commentId, String content, LocalDateTime updated_time, String routeName) {
        this.comment_id = commentId;
        this.content = content;
        this.updated_time = updated_time;
        this.routeName = routeName;
    }
}
