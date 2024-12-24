package com.airport.airport.controller;

import com.airport.airport.service.GcsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gcs")
public class GcsController {

    private final GcsService gcsService;

    @Autowired
    public GcsController(GcsService gcsService) {
        this.gcsService = gcsService;
    }

    // 유동인구 조회 API
    @GetMapping("/population/{dongCode}")
    public ResponseEntity<Map<String, Object>> getPopulation(
            @PathVariable String dongCode,
            @RequestParam String date) {
        Map<String, Object> data = gcsService.getPopulationData(date, dongCode);
        return ResponseEntity.ok(data);
    }

    // 도로 구간별 교통상황 조회 API
    @GetMapping("/traffic/{linkId}")
    public ResponseEntity<Map<String, Object>> getTraffic(
            @PathVariable String linkId,
            @RequestParam String date,
            @RequestParam String time) {
        Map<String, Object> data = gcsService.getLinkTraffic(date, linkId, time);
        return ResponseEntity.ok(data);
    }

    //도시별 혼잡도 조회
    @GetMapping("/traffic/{num}/all/{rank}")
    public ResponseEntity<List<Map<String, Object>>> getCityTraffic(
            @RequestParam String date,
            @RequestParam String hour,
            @PathVariable Integer num,
            @PathVariable Integer rank) {
        List<Map<String, Object>> cityTraffic = gcsService.getCityTraffic(date, hour, num, rank);
        return (ResponseEntity.ok(cityTraffic));
    }

    // 혼잡도 전부 조회
    @GetMapping("/traffic/all")
    public ResponseEntity<List<Map<String, Object>>> getAllTraffic(
            @RequestParam String date,
            @RequestParam String hour) {
        return ResponseEntity.ok(gcsService.getAllTraffic(date, hour));
    }


    // 경로 정보 조회 API
    @GetMapping("/routes/{dongCode}")
    public ResponseEntity<Map<String, Object>> getRoutes(
            @PathVariable String dongCode) {
        Map<String, Object> response = gcsService.getPopularRoutesToAirport(dongCode);
        return ResponseEntity.ok(response);
    }

    // 새로운 엔드포인트 추가 (전체 기간 조회)
    @GetMapping("/routes/all/{dongCode}")
    public ResponseEntity<Map<String, Object>> getAllRoutes(
            @PathVariable String dongCode) {
        Map<String, Object> response = gcsService.getPopularRoutesToAirport(dongCode);
        return ResponseEntity.ok(response);
    }
    //공항 예상 혼잡도 조회
    @GetMapping("/congestion")
    public ResponseEntity<Map<String, Object>> getCongestion(
            @RequestParam String date,
            @RequestParam String time) {
        Map<String, Object> response = gcsService.getCongestionData(date, time);
        return ResponseEntity.ok(response);
    }

}
