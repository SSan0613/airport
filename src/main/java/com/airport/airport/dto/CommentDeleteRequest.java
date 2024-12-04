package com.airport.airport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommentDeleteRequest {
    @Size(min = 1, message ="댓글 ID 목록은 최소 1개 이상이어야 합니다")
    @NotNull(message = "잘못된 요청")
    List<Long> commentIdList = new ArrayList<>();
}
