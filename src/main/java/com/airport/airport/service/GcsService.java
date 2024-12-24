package com.airport.airport.service;

import com.airport.airport.domain.Route;
import com.airport.airport.repository.RouteRepository;
import com.airport.airport.repository.VoteRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GcsService {
    private static final Logger logger = LoggerFactory.getLogger(GcsService.class);
    private final Storage storage;
    private final Map<String, List<double[]>> linkCoordinatesMap = new ConcurrentHashMap<>();
    private final Map<String, List<String[]>> csvCache = new ConcurrentHashMap<>();
    private volatile boolean isCoordinatesLoaded = false;
    // 법정동별 경로 정보를 캐싱
    private final Map<String, Map<String, Object>> routeCache = new ConcurrentHashMap<>();
    private volatile boolean isRoutesLoaded = false;

    @Autowired
    public GcsService(Storage storage, VoteRepository voteRepository, RouteRepository routeRepository) {
        this.storage = storage;
        this.voteRepository = voteRepository;
        this.routeRepository = routeRepository;
    }
    @Autowired
    private final VoteRepository voteRepository;
    @Autowired
    private final RouteRepository routeRepository;



    // 기본 클래스 정의

    private static class Segment {

        private String type;

        private String vehicleId;

        private String startStation;

        private final List<double[]> coordinates;

        private String startTime;

        private String endTime;
        private int duration;

        public Segment() {
            this.coordinates = new ArrayList<>();
        }

        public List<double[]> getCoordinates() { return coordinates; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }


        public void setType(String type) { this.type = type; }
        public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
        public void setStartStation(String startStation) { this.startStation = startStation; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public void setDuration(int duration) { this.duration = duration; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("type", type);
            if (vehicleId != null) map.put("vehicleId", vehicleId);
            if (startStation != null) map.put("Station", startStation);
            map.put("coordinates", coordinates);
            map.put("duration", duration);
            return map;
        }
    }

    private static class RouteInfo {
        String tripNo;
        String dongName;
        List<Map<String, Object>> segments;
        int totalTime;

        RouteInfo(String tripNo, List<Map<String, Object>> segments, int totalTime, String dongName) {
            this.tripNo = tripNo;
            this.segments = segments;
            this.totalTime = totalTime;
            this.dongName = dongName;
        }
    }

    // Helper 메서드들
    public byte[] downloadFileFromGcs(String bucketName, String objectName) {
        try {
            return Optional.ofNullable(storage.get(bucketName, objectName))
                    .map(Blob::getContent)
                    .orElseThrow(() -> new IllegalArgumentException("File not found: " + objectName));
        } catch (Exception e) {
            logger.error("Error downloading file: {}", e.getMessage());
            throw new RuntimeException("Error downloading file", e);
        }
    }

    public List<String[]> parseCsvFromGcs(String bucketName, String objectName) {
        return csvCache.computeIfAbsent(objectName, key -> {
            try {
                byte[] fileContent = downloadFileFromGcs(bucketName, key);
                Charset charset = detectCharsetWithBOM(fileContent);
                try (Reader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileContent), charset));
                     CSVReader csvReader = new CSVReader(reader)) {
                    return csvReader.readAll();
                }
            } catch (Exception e) {
                logger.error("Error parsing CSV: {}", e.getMessage());
                throw new RuntimeException("Error parsing CSV", e);
            }
        });
    }

    private Charset detectCharsetWithBOM(byte[] fileContent) {
        if (fileContent.length >= 3 && (fileContent[0] & 0xFF) == 0xEF && (fileContent[1] & 0xFF) == 0xBB && (fileContent[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        String content = new String(fileContent, StandardCharsets.ISO_8859_1);
        return content.contains("\uFFFD") ? Charset.forName("EUC-KR") : Charset.forName("CP949");
    }



    // 도로 링크별 교통 상태 조회 API
    public Map<String, Object> getLinkTraffic(String date, String linkId, String time) {
        loadLinkCoordinatesIfNeeded();

        return parseCsvFromGcs("pleaset", "표준노드링크 속도 통계(일별).csv").parallelStream()
                .skip(1)
                .filter(record -> record[0].equals(date) && record[3].equals(linkId))
                .findFirst()
                .map(record -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("roadType", record[8]);
                    result.put("linkId", linkId);
                    double speed = Double.parseDouble(record[Integer.parseInt(time) + 10]);
                    result.put("speed", speed);
                    result.put("status", getTrafficStatus(speed));
                    result.put("coordinates", linkCoordinatesMap.getOrDefault(linkId, new ArrayList<>()));
                    return result;
                })
                .orElseGet(HashMap::new);
    }

    // 유동인구 데이터 조회 API
    public Map<String, Object> getPopulationData(String date, String dongCode) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> hourlyData = parseCsvFromGcs("pleaset", "유동인구수정.csv").stream()
                .skip(1)
                .filter(record -> record[0].equals(date) && record[2].equals(dongCode))
                .map(record -> {
                    Map<String, Object> timeData = new HashMap<>();
                    timeData.put("time", record[4]);
                    timeData.put("population", Integer.parseInt(record[5]));
                    return timeData;
                })
                .collect(Collectors.toList());

        result.put("date", date);
        result.put("dongCode", dongCode);
        result.put("destination", "인천공항");
        result.put("hourlyData", hourlyData);
        return result;
    }
    // 도시별 혽잡도 조회 API
    public List<Map<String, Object>> getCityTraffic(String date, String hour, Integer num, Integer rank) {
        logger.info("getAllTraffic API called with date: {}, hour: {}", date, hour);
        List<Map<String, Object>> trafficData = new ArrayList<>();
        loadLinkCoordinatesIfNeeded(); // 링크 좌표 데이터 로드
        List<String[]> records;
        if(num== 1){        //경기도
            if(rank<101 || rank>108) logger.error("도로등급 범위 벗어남");
            HashMap<Integer, String> map = new HashMap<>();
            map.put(101, "고속국도");
            map.put(102, "도시고속도로");
            map.put(103, "일반국도");
            map.put(104, "특별´광역시도");
            map.put(105, "국가지원지방도");
            map.put(106, "지방도");
            map.put(107, "시군도");
            map.put(108, "기타");
            String roadRank = map.get(rank);
            records  = parseCsvFromGcs("pleaset", "경기도_도로_통행속도.csv");
            int speedIndex = Integer.parseInt(hour) + 10;
            String cachedRoadRank = roadRank;

            records.parallelStream() // 병렬 스트림 사용
                    .skip(1) // 헤더 제외
                    .filter(record -> cachedRoadRank.equals(record[9]) && record[0].equals(date)) // 필터링 조건
                    .forEach(record -> {
                        try {
                            String linkId = record[3];
                            double speed = Double.parseDouble(record[speedIndex]); // 속도 계산

                            // 도로 정보 생성
                            Map<String, Object> roadInfo = new HashMap<>(8); // 초기 크기 지정
                            roadInfo.put("linkId", linkId);
                            roadInfo.put("roadName", record[2]);
                            roadInfo.put("roadType", record[9]);
                            roadInfo.put("distance", record[7]);
                            roadInfo.put("speed", speed);
                            roadInfo.put("trafficStatus", getTrafficStatus(speed));
                            roadInfo.put("coordinates", linkCoordinatesMap.getOrDefault(linkId, Collections.emptyList()));

                            // 동기화하여 trafficData에 추가
                            synchronized (trafficData) {
                                trafficData.add(roadInfo);
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid speed value: {}", record[speedIndex]);
                        } catch (Exception e) {
                            logger.error("Error processing record: {}", e.getMessage());
                        }
                    });

            logger.info("getAllTraffic processed successfully. Total records: {}", trafficData.size());


        }
        else if(num==2){        //인천
            if(rank<101 || rank>106) logger.error("도로등급 범위 벗어남");
            HashMap<Integer, String> map = new HashMap<>();
            map.put(101,"특별·광역시도");
            map.put(102,"일반국도");
            map.put(103,"시·군도");
            map.put(104,"고속국도");
            map.put(105,"지방도");
            map.put(106,"국가지원지방도");
            String roadRank = map.get(rank);
            records  = parseCsvFromGcs("pleaset", "incheon_output.csv");
            int speedIndex = Integer.parseInt(hour) + 10;
            String cachedRoadRank = roadRank;

            records.stream()
                    .skip(1) // 헤더 제외
                    .filter(record -> cachedRoadRank.equals(record[9]) && record[0].equals(date)) // 필터링
                    .parallel() // 병렬 처리
                    .forEach(record -> {
                        try {
                            String linkId = record[3];
                            double speed = Double.parseDouble(record[speedIndex]);

                            Map<String, Object> roadInfo = new HashMap<>(8);
                                roadInfo.put("linkId", linkId);
                            roadInfo.put("roadName", record[2]);
                            roadInfo.put("roadType", record[9]);
                            roadInfo.put("distance", record[7]);
                            roadInfo.put("speed", speed);
                            roadInfo.put("trafficStatus", getTrafficStatus(speed));
                            roadInfo.put("coordinates", linkCoordinatesMap.getOrDefault(linkId, Collections.emptyList()));

                            synchronized (trafficData) { // 동기화
                                trafficData.add(roadInfo);
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid speed value: {}", record[speedIndex]);
                        } catch (Exception e) {
                            logger.error("Error processing record: {}", e.getMessage());
                        }
                    });

            logger.info("getAllTraffic processed successfully. Total records: {}", trafficData.size());
        }
        else if(num==3){        //서울
            if(rank<101 || rank>104) logger.error("도로등급 범위 벗어남");
            HashMap<Integer, String> map = new HashMap<>();
            map.put(101,"보조간선도로");
            map.put(102,"주간선도로");
            map.put(103,"기타도로");
            map.put(104,"도시고속도로");
            String roadRank = map.get(rank);
            records  = parseCsvFromGcs("pleaset", "output.csv");
            int speedIndex = Integer.parseInt(hour) + 13;
            String cachedRoadRank = map.get(rank);

            records.parallelStream()
                    .skip(1) // 헤더 제외
                    .filter(record -> cachedRoadRank.equals(record[10]) && record[0].equals(date))
                    .forEach(record -> {
                        try {
                            String linkId = record[4];
                            double speed = Double.parseDouble(record[speedIndex]);

                            Map<String, Object> roadInfo = new HashMap<>(8);
                            roadInfo.put("linkId", linkId);
                            roadInfo.put("roadName", record[2]);
                            roadInfo.put("roadType", record[10]);
                            roadInfo.put("distance", record[8]);
                            roadInfo.put("speed", speed);
                            roadInfo.put("trafficStatus", getTrafficStatus(speed));
                            roadInfo.put("coordinates", linkCoordinatesMap.getOrDefault(linkId, Collections.emptyList()));

                            synchronized (trafficData) {
                                trafficData.add(roadInfo); // 동기화 필요
                            }
                        } catch (Exception e) {
                            logger.error("Error processing record: {}", e.getMessage());
                        }
                    });

            logger.info("getAllTraffic processed successfully. Total records: {}", trafficData.size());
        }

        return trafficData;
    }

    private List<String[]> csvReader(String s) {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(s))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(","); // 쉼표로 구분
                records.add(values); // 각 행을 String 배열로 추가
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }
    //공항 혼잡도 데이터 조회
    public Map<String, Object> getCongestionData(String date, String time) {
        String formattedTime = String.format("%d:00", Integer.parseInt(time));
        String targetDateTime = date + " " + formattedTime;

        logger.info("Fetching congestion data for targetDateTime: {}", targetDateTime);

        List<String[]> records = parseCsvFromGcs("pleaset", "혼잡도예측.csv");

        String[] currentRecord = records.stream()
                .filter(record -> record[0].equals(targetDateTime))
                .findFirst()
                .orElse(null);

        if (currentRecord == null) {
            logger.warn("No record found for targetDateTime: {}", targetDateTime);
            throw new IllegalArgumentException("No congestion data found for the given date and time");
        }

        Map<String, Object> result = new LinkedHashMap<>(); // 순서 유지
        result.put("date", date);
        result.put("time", formattedTime);

        // 현재 혼잡도 처리
        double currentCount = Double.parseDouble(currentRecord[1]); // count 열
        result.put("current_count", Math.round(currentCount)); // 반올림 처리

        // 1~6시간 예측 값 추가
        Map<String, Integer> predictions = new LinkedHashMap<>(); // 순서 유지
        for (int i = 2; i <= 7; i++) {
            if (!currentRecord[i].isEmpty()) {
                double prediction = Double.parseDouble(currentRecord[i]);
                predictions.put("+" + (i - 1) + "h", (int) Math.round(prediction)); // 반올림 후 정수 변환
            } else {
                predictions.put("+" + (i - 1) + "h", null); // 빈 값 처리
            }
        }
        result.put("predictions", predictions);

        return result;
    }


    // 혼잡도 전부 조회 API
    public List<Map<String, Object>> getAllTraffic(String date, String hour) {
        logger.info("getAllTraffic API called with date: {}, hour: {}", date, hour);

        loadLinkCoordinatesIfNeeded(); // 링크 좌표 데이터 로드

        List<Map<String, Object>> trafficData = new ArrayList<>();
        List<String[]> records = parseCsvFromGcs("pleaset", "표준노드링크 속도 통계(일별).csv");

        for (int i = 1; i < records.size(); i++) { // 헤더 스킵
            String[] record = records.get(i);
            if (!record[0].equals(date)) continue; // date 필터링

            try {
                String linkId = record[3];
                Map<String, Object> roadInfo = new HashMap<>();
                roadInfo.put("linkId", linkId);
                roadInfo.put("roadName", record[2]);
                roadInfo.put("roadType", record[8]);
                roadInfo.put("distance", record[7]);

                // 시간별 속도 값 계산
                double speed = Double.parseDouble(record[Integer.parseInt(hour) + 10]);
                roadInfo.put("speed", speed);
                roadInfo.put("trafficStatus", getTrafficStatus(speed));

                // 좌표 데이터 추가
                roadInfo.put("coordinates", linkCoordinatesMap.getOrDefault(linkId, new ArrayList<>()));

                trafficData.add(roadInfo);
            } catch (Exception e) {
                logger.error("Error processing record at index {}: {}", i, e.getMessage());
            }
        }

        logger.info("getAllTraffic processed successfully. Total records: {}", trafficData.size());
        return trafficData;
    }


    private String getTrafficStatus(double speed) {
        if (speed >= 50) return "원활";
        if (speed >= 30) return "서행";
        return "정체";
    }

    private void loadLinkCoordinatesIfNeeded() {
        if (!isCoordinatesLoaded) {
            synchronized (this) {
                if (!isCoordinatesLoaded) {
                    loadLinkCoordinates();
                    isCoordinatesLoaded = true;
                }
            }
        }
    }

    private void loadLinkCoordinates() {
        try {
            List<String[]> records = parseCsvFromGcs("pleaset", "링크좌표.csv");

            records.parallelStream()
                    .skip(1)
                    .forEach(record -> {
                        try {
                            String linkId = record[0];
                            String geometryStr = record[record.length - 1]; // 마지막 열의 WGS84 좌표

                            // LINESTRING ( ) 제거 및 좌표 파싱
                            String coordsStr = geometryStr.substring(
                                    geometryStr.indexOf("(") + 1,
                                    geometryStr.lastIndexOf(")")
                            ).trim();

                            // 좌표 쌍 분리 및 변환
                            String[] coordPairs = coordsStr.split(",");
                            List<double[]> coordinates = new ArrayList<>();

                            for (String pair : coordPairs) {
                                String[] latLon = pair.trim().split(" ");
                                double lat = Double.parseDouble(latLon[0]);
                                double lon = Double.parseDouble(latLon[1]);
                                coordinates.add(new double[]{lat, lon});
                            }

                            linkCoordinatesMap.put(linkId, coordinates);

                        } catch (Exception e) {
                            logger.error("레코드 처리 중 오류: {}, 레코드: {}", e.getMessage(), Arrays.toString(record));
                        }
                    });

            logger.info("링크좌표 로드 완료. 총 링크 수: {}", linkCoordinatesMap.size());
        } catch (Exception e) {
            logger.error("링크좌표 로드 중 오류 발생: {}", e.getMessage());
            throw e;
        }
    }

    // 유사 경로 찾기 및 클러스터링
    private List<List<RouteInfo>> groupSimilarRoutes(List<RouteInfo> routes) {
        if (routes.isEmpty()) return new ArrayList<>();

        double eps = 2.0; // 2km
        int minPts = 2; // 최소 2개의 경로

        List<List<RouteInfo>> clusters = performDBSCAN(routes, eps, minPts);
        clusters.sort((c1, c2) -> Integer.compare(c2.size(), c1.size()));
        return clusters;
    }

    private List<List<RouteInfo>> performDBSCAN(List<RouteInfo> routes, double eps, int minPts) {
        List<TrajectoryPoint> points = routes.parallelStream()
                .map(TrajectoryPoint::new)
                .toList();

        // 이웃 찾기
        points.parallelStream().forEach(p -> {
            for (TrajectoryPoint q : points) {
                if (p != q && calculatePathSimilarity(p.route, q.route) <= eps) {
                    p.neighbors.add(q);
                }
            }
        });

        // 클러스터링
        int clusterId = 0;
        for (TrajectoryPoint p : points) {
            if (p.visited) continue;
            p.visited = true;
            if (p.neighbors.size() >= minPts) {
                expandCluster(p, clusterId++, minPts);
            }
        }

        // 결과 변환
        Map<Integer, List<RouteInfo>> clusters = new HashMap<>();
        for (TrajectoryPoint p : points) {
            if (p.clusterId != -1) {
                clusters.computeIfAbsent(p.clusterId, k -> new ArrayList<>()).add(p.route);
            }
        }
        return new ArrayList<>(clusters.values());
    }

    private void expandCluster(TrajectoryPoint p, int clusterId, int minPts) {
        p.clusterId = clusterId;
        Queue<TrajectoryPoint> queue = new LinkedList<>(p.neighbors);
        while (!queue.isEmpty()) {
            TrajectoryPoint q = queue.poll();
            if (!q.visited) {
                q.visited = true;
                if (q.neighbors.size() >= minPts) {
                    queue.addAll(q.neighbors);
                }
            }
            if (q.clusterId == -1) {
                q.clusterId = clusterId;
            }
        }
    }

    private static class TrajectoryPoint {
        RouteInfo route;
        List<TrajectoryPoint> neighbors;
        boolean visited;
        int clusterId;

        public TrajectoryPoint(RouteInfo route) {
            this.route = route;
            this.neighbors = new ArrayList<>();
            this.visited = false;
            this.clusterId = -1;
        }
    }

    // 경로 유사도 계산
    private double calculatePathSimilarity(RouteInfo route1, RouteInfo route2) {
        List<double[]> path1 = extractFullPath(route1);
        List<double[]> path2 = extractFullPath(route2);
        double dtwDist = calculateDTWDistance(path1, path2);
        double frechetDist = calculateFrechetDistance(path1, path2);
        return (dtwDist * 0.5) + (frechetDist * 0.5);
    }

    private List<double[]> extractFullPath(RouteInfo route) {
        return route.segments.stream()
                .flatMap(segment -> ((List<double[]>) segment.get("coordinates")).stream())
                .collect(Collectors.toList());
    }

    // DTW 거리 계산
    private double calculateDTWDistance(List<double[]> path1, List<double[]> path2) {
        int n = path1.size();
        int m = path2.size();
        double[][] dtw = new double[2][m + 1];
        Arrays.fill(dtw[0], Double.POSITIVE_INFINITY);
        dtw[0][0] = 0;

        for (int i = 1; i <= n; i++) {
            int curr = i % 2;
            int prev = (i - 1) % 2;
            dtw[curr][0] = Double.POSITIVE_INFINITY;

            for (int j = 1; j <= m; j++) {
                double cost = calculateDistance(path1.get(i - 1)[0], path1.get(i - 1)[1], path2.get(j - 1)[0], path2.get(j - 1)[1]);
                dtw[curr][j] = cost + Math.min(Math.min(dtw[prev][j], dtw[curr][j - 1]), dtw[prev][j - 1]);
            }
        }
        return dtw[n % 2][m];
    }

    // Frechet distance 계산
    private double calculateFrechetDistance(List<double[]> path1, List<double[]> path2) {
        int n = path1.size();
        int m = path2.size();
        double[][] ca = new double[n][m];
        for (double[] row : ca) {
            Arrays.fill(row, -1);
        }
        return c(n - 1, m - 1, path1, path2, ca);
    }

    private double c(int i, int j, List<double[]> path1, List<double[]> path2, double[][] ca) {
        if (ca[i][j] > -1) {
            return ca[i][j];
        }
        if (i == 0 && j == 0) {
            ca[i][j] = calculateDistance(
                    path1.getFirst()[0], path1.getFirst()[1],
                    path2.getFirst()[0], path2.getFirst()[1]
            );
        } else if (i > 0 && j == 0) {
            ca[i][j] = Math.max(
                    c(i - 1, 0, path1, path2, ca),
                    calculateDistance(
                            path1.get(i)[0], path1.get(i)[1],
                            path2.getFirst()[0], path2.getFirst()[1]
                    )
            );
        } else if (i == 0 && j > 0) {
            ca[i][j] = Math.max(
                    c(0, j - 1, path1, path2, ca),
                    calculateDistance(
                            path1.getFirst()[0], path1.getFirst()[1],
                            path2.get(j)[0], path2.get(j)[1]
                    )
            );
        } else {
            ca[i][j] = Math.max(
                    Math.min(
                            Math.min(c(i - 1, j, path1, path2, ca), c(i - 1, j - 1, path1, path2, ca)),
                            c(i, j - 1, path1, path2, ca)
                    ),
                    calculateDistance(
                            path1.get(i)[0], path1.get(i)[1],
                            path2.get(j)[0], path2.get(j)[1]
                    )
            );
        }
        return ca[i][j];
    }

    private void loadRoutesIfNeeded() {
        if (!isRoutesLoaded) {
            synchronized (this) {
                if (!isRoutesLoaded) {
                    loadRoutes();
                    isRoutesLoaded = true;
                }
            }
        }
    }
    private void loadRoutes() {
        try {
            byte[] content = downloadFileFromGcs("pleaset", "analyzed_routes_real.json");


            String jsonContent = new String(content, StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> allRoutes = mapper.readValue(jsonContent, new TypeReference<>() {});

            allRoutes.forEach((dongCode, routeInfo) ->
                    routeCache.put(dongCode, (Map<String, Object>) routeInfo));

        } catch (Exception e) {
            logger.error("Error loading routes: {}", e.getMessage());
            throw new RuntimeException("Error loading routes", e);
        }
    }


    public Map<String, Object> getPopularRoutesToAirport(String dongCode) {

        loadRoutesIfNeeded();
        List<String[]> dongCodeMapping = parseCsvFromGcs("pleaset", "법정동코드목록.csv");
        String dongName = findDongName(dongCode, dongCodeMapping);

        Map<String, Object> routeData = routeCache.getOrDefault(dongCode, createEmptyResponse());
        List<Map<String,Object>> patterns = (List<Map<String, Object>>) routeData.get("patterns");
        int count=1;
        for (Map<String, Object> pattern : patterns) {
            String routeId = (String) pattern.get("route_id");
            if (routeId != null) {
                if (routeRepository.existsByRouteId(routeId)) {
                    int positive = voteRepository.findPositiveByRoute_id(routeId).orElse(0);
                    int negative = voteRepository.findNegativeByRoute_id(routeId).orElse(0);
                    // 패턴에 업데이트
                    pattern.put("positive", positive);
                    pattern.put("negative", negative);
                } else {
                    // 추천수 초기화
                    Route newRoute = new Route(routeId,dongName+" 경로 " + count++);
                    routeRepository.save(newRoute);
                    pattern.put("positive", 0);
                    pattern.put("negative", 0);
                }
            }
        }
        routeCache.put(dongCode,routeData);
        return routeData;
    }

    private String findDongName(String dongCode, List<String[]> dongCodeMapping) {
        for (String[] strings : dongCodeMapping) {
            if (strings[0].equals(dongCode)) {
                return strings[3];
            }
        }
        return null;
    }

    private Map<String, Object> createEmptyResponse() {
        Map<String, Object> stringObjectMap = null;
        return stringObjectMap;
    }

    private Map<String, List<String[]>> filterAndSortTrips(
            Map<String, List<String[]>> tripGroups, String dongCode) {
        Map<String, List<String[]>> validTrips = new HashMap<>();
        for (Map.Entry<String, List<String[]>> entry : tripGroups.entrySet()) {
            List<String[]> tripRecords = entry.getValue();
            tripRecords.sort(Comparator.comparing(a -> a[5]));
            if (tripRecords.getFirst()[tripRecords.getFirst().length - 1].equals(dongCode)) {
                validTrips.put(entry.getKey(), tripRecords);
            }
        }
        return validTrips;
    }

    private List<Map<String, Object>> createSegments(List<String[]> records) {
        double avgWalkingSpeed = calculateAverageNonBusSpeed(records);
        if (avgWalkingSpeed > 15.0) {
            return createCarSegment(records);
        }
        return createBusWalkingSegments(records);
    }

    private double calculateAverageNonBusSpeed(List<String[]> records) {
        List<Double> speeds = new ArrayList<>();
        for (String[] record : records) {
            String busId = record[17];
            if (busId == null || busId.isEmpty()) {
                double speed = Double.parseDouble(record[2]);
                if (speed > 0) {
                    speeds.add(speed);
                }
            }
        }
        return speeds.isEmpty() ? 0.0 : speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private int calculateAverageTime(List<RouteInfo> group) {
        return (int) group.stream()
                .mapToInt(r -> calculateTotalTime(r.segments))
                .filter(time -> time > 0) // 0 또는 음수 시간 제외
                .average()
                .orElse(0);
    }

    private int calculateTotalTime(List<Map<String, Object>> segments) {
        return segments.stream()
                .mapToInt(segment -> (int) segment.get("duration"))
                .sum();
    }

    private List<Map<String, Object>> createCarSegment(List<String[]> records) {
        Segment segment = new Segment();
        segment.setType("자동차");
        segment.setStartTime(records.getFirst()[5]);
        segment.setEndTime(records.getLast()[13]);
        for (String[] record : records) {
            double[] coord = new double[]{
                    Double.parseDouble(record[7]),
                    Double.parseDouble(record[8])
            };
            if (segment.getCoordinates().isEmpty() || !areCoordinatesEqual(segment.getCoordinates().getLast(), coord)) {
                segment.getCoordinates().add(coord);
            }
        }
        segment.setDuration(calculateDuration(segment.getStartTime(), segment.getEndTime()));
        return Collections.singletonList(segment.toMap());
    }

    private List<Map<String, Object>> createBusWalkingSegments(List<String[]> records) {
        List<Segment> segments = new ArrayList<>();
        Segment currentSegment = null;
        String currentBusId = null;
        int segmentStartIndex = 0;
        for (int i = 0; i < records.size(); i++) {
            String[] record = records.get(i);
            String busId = record[17];
            String station = record[19];
            boolean shouldStartNewSegment = false;
            if (currentSegment == null) {
                shouldStartNewSegment = true;
            } else if (station != null && !station.isEmpty() && (currentBusId == null || !currentBusId.equals(busId))) {
                shouldStartNewSegment = true;
            } else if (currentBusId != null && busId == null) {
                shouldStartNewSegment = true;
            }
            if (shouldStartNewSegment) {
                if (currentSegment != null) {
                    String[] startRecord = records.get(segmentStartIndex);
                    String[] endRecord = records.get(i - 1);
                    currentSegment.setDuration(calculateDuration(startRecord[5], endRecord[13]));
                    segments.add(currentSegment);
                }
                segmentStartIndex = i;
                currentSegment = new Segment();
                if (station != null && !station.isEmpty()) {
                    currentSegment.setType("버스");
                    currentSegment.setVehicleId(busId);
                    currentSegment.setStartStation(station);
                    currentBusId = busId;
                } else {
                    currentSegment.setType("도보");
                    currentBusId = null;
                }
                currentSegment.setStartTime(record[5]);
                double[] coord = new double[]{
                        Double.parseDouble(record[7]),
                        Double.parseDouble(record[8])
                };
                currentSegment.getCoordinates().add(coord);
                double[] endCoord = new double[]{
                        Double.parseDouble(record[15]),
                        Double.parseDouble(record[16])
                };
                if (!areCoordinatesEqual(coord, endCoord)) {
                    currentSegment.getCoordinates().add(endCoord);
                }
                continue;
            }
            double[] newCoord = new double[]{
                    Double.parseDouble(record[7]),
                    Double.parseDouble(record[8])
            };
            if (!areCoordinatesEqual(
                    currentSegment.getCoordinates().getLast(),
                    newCoord)) {
                currentSegment.getCoordinates().add(newCoord);
            }
        }
        if (currentSegment != null) {
            String[] startRecord = records.get(segmentStartIndex);
            String[] endRecord = records.getLast();
            currentSegment.setDuration(calculateDuration(startRecord[5], endRecord[13]));
            segments.add(currentSegment);
        }
        return segments.stream()
                .map(Segment::toMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> createFinalRoutes(List<List<RouteInfo>> clusters) {
        int [] counter = {1};
        return clusters.stream()
                .limit(3)
                .map(cluster -> {
                    RouteInfo representative = selectRepresentativeRoute(cluster);
                    int averageTime = calculateAverageTime(cluster);
                    List<Map<String, Object>> adjustedSegments = adjustSegmentDurations(representative.segments, averageTime);

                    String tripNo =representative.tripNo.replace("TRIP_","");
                    String routeId = tripNo;
                    String dongName = representative.dongName;


                    if (!routeRepository.existsByRouteId(routeId)) {
                        Route newRoute = new Route(routeId,dongName+" 경로 "+counter[0]++);
                        routeRepository.save(newRoute);
                    }
                    int positive = voteRepository.findPositiveByRoute_id(routeId)
                            .orElse(0);
                    int negative = voteRepository.findNegativeByRoute_id(routeId)
                            .orElse(0);

                    return Map.of(
                            "routeId", "ROUTE_" + representative.tripNo,
                            "segments", adjustedSegments,
                            "frequency", cluster.size(),
                            "averageTime", averageTime,
                            "positive",positive,
                            "negative",negative
                    );
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> adjustSegmentDurations(List<Map<String, Object>> segments, int averageTime) {
        double totalDuration = segments.stream()
                .mapToInt(segment -> (int) segment.get("duration"))
                .filter(duration -> duration <= averageTime * 2) // 명백한 이상치 제거
                .sum();
        double adjustmentFactor = averageTime / totalDuration;

        return segments.stream().map(segment -> {
            Map<String, Object> adjustedSegment = new HashMap<>(segment);
            int originalDuration = (int) segment.get("duration");
            if (originalDuration <= averageTime * 2) {
                int adjustedDuration = (int) Math.round(originalDuration * adjustmentFactor);
                adjustedSegment.put("duration", adjustedDuration);
            } else {
                adjustedSegment.put("duration", averageTime / segments.size()); // 이상치를 평균 시간으로 대체
            }
            return adjustedSegment;
        }).collect(Collectors.toList());
    }


    private RouteInfo selectRepresentativeRoute(List<RouteInfo> cluster) {
        if (cluster.size() == 1) {
            return cluster.getFirst();
        }
        double minTotalDistance = Double.MAX_VALUE;
        RouteInfo representative = null;
        for (RouteInfo route : cluster) {
            double totalDistance = 0;
            for (RouteInfo other : cluster) {
                if (route != other) {
                    totalDistance += calculatePathSimilarity(route, other);
                }
            }
            if (totalDistance < minTotalDistance) {
                minTotalDistance = totalDistance;
                representative = route;
            }
        }
        return representative;
    }



    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private int calculateDuration(String startTime, String endTime) {
        LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return (int) ChronoUnit.MINUTES.between(start, end);
    }

    private static final double EPSILON = 0.00001;

    private boolean areCoordinatesEqual(double[] coord1, double[] coord2) {
        if (coord1.length != 2 || coord2.length != 2) {
            throw new IllegalArgumentException("Coordinates must have exactly 2 dimensions (latitude, longitude).");
        }
        return Math.abs(coord1[0] - coord2[0]) < EPSILON &&
                Math.abs(coord1[1] - coord2[1]) < EPSILON;
    }


}