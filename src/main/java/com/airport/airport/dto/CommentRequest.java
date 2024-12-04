package com.airport.airport.dto;

import com.airport.airport.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommentRequest {
    @NotBlank(message = "내용을 입력해 주세요")
    private String content;
}
