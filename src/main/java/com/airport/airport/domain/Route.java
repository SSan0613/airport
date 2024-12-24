package com.airport.airport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@RequiredArgsConstructor
@Getter
@Table(name = "route")
public class Route {
    @Id
    @Column(name = "route_id")
    private String routeId;

    @Column
    private String name;

    public Route(String routeId) {
        this.routeId = routeId;
    }
    public Route(String routeId, String name) {
        this.routeId = routeId;
        this.name = name;
    }
}
