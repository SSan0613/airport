# 인천공항 이용객 동선 분석 프로젝트


## 1. 제작기간 & 참여 인원
 2024.09.08 ~ 2024.12.01
 
 백엔드 2인, 프론트 1인, AI 1인으로 구성된 팀 프로젝트입니다.

저는 JWT 활용한 로그인 기능, 댓글 CRUD, 경로 추천/비추천, 도로 혼잡도 반환 API 개발 및 인프라 구성을 담당하였습니다.

## 2. 사용 기술

- JAVA 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- JWT
- Spring Security


## 3. API 명세서

프론트엔드와 원활한 소통을 위해 사용자 관련 api 명세서를 작성하였습니다.
<img width = "80%" src = "https://private-user-images.githubusercontent.com/142812547/400655687-77e72276-0d65-480a-bc87-2f134123790d.PNG?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MzYyMzEwMTUsIm5iZiI6MTczNjIzMDcxNSwicGF0aCI6Ii8xNDI4MTI1NDcvNDAwNjU1Njg3LTc3ZTcyMjc2LTBkNjUtNDgwYS1iYzg3LTJmMTM0MTIzNzkwZC5QTkc_WC1BbXotQWxnb3JpdGhtPUFXUzQtSE1BQy1TSEEyNTYmWC1BbXotQ3JlZGVudGlhbD1BS0lBVkNPRFlMU0E1M1BRSzRaQSUyRjIwMjUwMTA3JTJGdXMtZWFzdC0xJTJGczMlMkZhd3M0X3JlcXVlc3QmWC1BbXotRGF0ZT0yMDI1MDEwN1QwNjE4MzVaJlgtQW16LUV4cGlyZXM9MzAwJlgtQW16LVNpZ25hdHVyZT1jY2MwZmE1ZjIzOTI4OGRmYmVkZmMwMGQ3NzdlNTU4M2EzZDFhZTYzYWMxZjFmOTYzNzZiOTFhY2Y1NmVhMWMwJlgtQW16LVNpZ25lZEhlYWRlcnM9aG9zdCJ9.GhuJY1zwxBXdTbib6ENlV4HwREPIFzPqe7ht-m17E74">
<br/>
- 노션 링크 : https://carnation-pedestrian-278.notion.site/api-118f4dcd5263801bb290c4b888f7b69e?pvs=74


## 4. ERD

<img width = "70%" src = "https://private-user-images.githubusercontent.com/142812547/400642820-0689f2a0-1cd0-4fe7-b9d8-686176e57c30.PNG?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MzYyMjcyMTgsIm5iZiI6MTczNjIyNjkxOCwicGF0aCI6Ii8xNDI4MTI1NDcvNDAwNjQyODIwLTA2ODlmMmEwLTFjZDAtNGZlNy1iOWQ4LTY4NjE3NmU1N2MzMC5QTkc_WC1BbXotQWxnb3JpdGhtPUFXUzQtSE1BQy1TSEEyNTYmWC1BbXotQ3JlZGVudGlhbD1BS0lBVkNPRFlMU0E1M1BRSzRaQSUyRjIwMjUwMTA3JTJGdXMtZWFzdC0xJTJGczMlMkZhd3M0X3JlcXVlc3QmWC1BbXotRGF0ZT0yMDI1MDEwN1QwNTE1MThaJlgtQW16LUV4cGlyZXM9MzAwJlgtQW16LVNpZ25hdHVyZT03ZGZjNWE0MDIwYTEzY2NhZTQwNzY5MTJiMzJjYzZkNjhjMWQ2ZTMwMzQyNWU5MjYzOTdlMDA4OTE0ZDgyMjBmJlgtQW16LVNpZ25lZEhlYWRlcnM9aG9zdCJ9.H2_CZQYUDL153OPFONTeOgYgdrxJ9rcwJWkmhJiz_UE">

## 5. 시스템 구성도

<img width = "70%" src = "https://private-user-images.githubusercontent.com/142812547/400695314-cf6a7eff-4dc4-4bd9-8f71-8a315a8ac83a.PNG?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MzcxMjgzNjIsIm5iZiI6MTczNzEyODA2MiwicGF0aCI6Ii8xNDI4MTI1NDcvNDAwNjk1MzE0LWNmNmE3ZWZmLTRkYzQtNGJkOS04ZjcxLThhMzE1YThhYzgzYS5QTkc_WC1BbXotQWxnb3JpdGhtPUFXUzQtSE1BQy1TSEEyNTYmWC1BbXotQ3JlZGVudGlhbD1BS0lBVkNPRFlMU0E1M1BRSzRaQSUyRjIwMjUwMTE3JTJGdXMtZWFzdC0xJTJGczMlMkZhd3M0X3JlcXVlc3QmWC1BbXotRGF0ZT0yMDI1MDExN1QxNTM0MjJaJlgtQW16LUV4cGlyZXM9MzAwJlgtQW16LVNpZ25hdHVyZT1kZTQ5NjllYzE5NDhiYWRkYjk5NDE4ZWQ3MTU4YzU2MmEzNDNkZjRkNTU1MDBiN2Q4YzFhYmIwMWM2Mjk5ODI0JlgtQW16LVNpZ25lZEhlYWRlcnM9aG9zdCJ9.l_7ktLqhd8dWWUzcAQYSQm3h6XcnPkty8tQCMKi0hiM">


## 6. 트러블 슈팅

<br/>

 ### 6-1. 도로혼잡도 API 성능 최적화

<br/> 

- 도로 혼잡도 API를 개발하면서, 초기 설계에서는 수도권 전체 도로 데이터를 클라이언트에 반환하도록 구현했습니다.

    - 문제상황:

        - 데이터량 과다로 인해 클라이언트에서 지도에 혼잡도를 시각화하는 속도가 매우 느려졌습니다. 
        
        - 또한 서버 응답 시간이 지연되는 현상이 발생하였습니다.

    - 해결 방안:
        - API를 각 도시별, 도로등급별로 세분화하여 클라이언트가 줌 레벨에 따라 필요한 도시별, 도로 등급만 요청하도록 설계하였습니다.
        

         - 줌 레벨이 낮을 때는 고속국도와 고속도로 등 주요 도로만 반환.
         - 줌 레벨이 높을 때는 해당 도시 내의 도로 데이터를 반환.
    

<BR/>
<details>
<summary> 개선된 코드</summary>

- 기존 컨트롤러
<br/>
<br/>

```java
    // 혼잡도 전부 조회
    @GetMapping("/traffic/all")
    public ResponseEntity<List<Map<String, Object>>> getAllTraffic(
            @RequestParam String date,
            @RequestParam String hour) {
        return ResponseEntity.ok(gcsService.getAllTraffic(date, hour));
    }
```

 
<BR/>
- 수정된 컨트롤러
<br/>
<br/>

```java
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
```
</details>
<br/>

- 해당 api를 수정한 결과 응답속도가 50% 이상 개선되는 효과가 있었습니다.


## 7. 시연 영상
![PORT_AL+시연+영상+(1)+(1) (1)](https://github.com/user-attachments/assets/5028971a-1f84-4213-8a3a-63830c405ff9)
