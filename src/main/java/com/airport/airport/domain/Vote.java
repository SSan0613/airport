package com.airport.airport.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@RequiredArgsConstructor
@Getter
@Table(name = "vote")
public class Vote {

    @Id
    @GeneratedValue
    private Long voteId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @Column
    private int positive;

    @Column
    private int negative;

    //테스트
    public Vote(int negative, int positive, Route route, User user) {
        this.negative = negative;
        this.positive = positive;
        this.route = route;
        this.user = user;
    }

    public void decreasePositive() {
            positive-=1;
    }

    public void decreaseNegative() {
            negative-=1;
    }

    public void increasePositive() {
            positive+=1;
    }

    public void increaseNegative() {
            negative+=1;
    }
}
