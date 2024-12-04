package com.airport.airport.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Entity
@RequiredArgsConstructor
@Getter
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long commentId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="route_id")
    private Route route;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;

    @Column
    private String content;

    @Column
    private LocalDateTime updatedTime;

    @PrePersist
    public void set_time() {
        this.updatedTime = LocalDateTime.now().withNano(0);
    }

/*    @PreUpdate            //댓글 수정 시 수정한 시간으로 바꾸기
    public void update_time() {
        this.updatedTime = LocalDateTime.now().withNano(0);
    }*/

    public Comment(Route route, User user, String content) {
        this.route = route;
        this.user = user;
        this.content = content;
    }

    public void updateContent(String newContent) {
        content = newContent;
    }
}
