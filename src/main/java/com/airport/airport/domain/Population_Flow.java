package com.airport.airport.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.OffsetDateTime;

@Entity
@Getter
@RequiredArgsConstructor
@Table(name = "population_flow")
public class Population_Flow {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private float movement_speed;
    @Column
    private String movement_type;
    @Column
    private float origin_x;
    @Column
    private float origin_y;
    @Column
    private String origin_area;
    @Column
    private OffsetDateTime origin_time;
    @Column
    private String origin_type;
    @Column
    private OffsetDateTime destination_time;
    @Column
    private float destination_x;
    @Column
    private float destination_y;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_cell_id")        //csv 파일 읽으면서 정보 넣을떄는 기존 셀 있으면 찾아 매핑하거나 없으면 셀 생성 해야할듯???
    private Cell origin_cell_id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_cell_id")
    private Cell destination_cell_id;
}
