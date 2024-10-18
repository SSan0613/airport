package com.airport.airport.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor
@Table(name="Route_stop")
public class Route_Stop {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long stop_id;
    @Column
    private Long route_id;
    @Column
    private Long cell_id;
    @Column
    private int stop_order;
    @Column
    private int stop_name;
    @Column
    private int route_id2;
    @Column
    private int cell_id2;
}
