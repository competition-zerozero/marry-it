# AGENTS.md

## 프로젝트 개요

`marry-it`은 웨딩플래너를 위한 Spring Boot 기반 업무 관리 및 AI Agent 서비스다.

개인 웨딩플래너와 여러 플래너가 소속된 웨딩 업체 모두 사용할 수 있으며, 고객, 업체, 일정, 플래너의 경험과 노하우를 하나의 서비스에서 관리한다.

서비스의 핵심은 단순한 고객 관리나 업체 추천이 아니다.

고객 정보, 업체 정보, 플래너의 경험과 노하우, 일정, 위치 정보를 연결하고 AI Agent가 MCP Tool을 활용하여 웨딩플래너의 실제 업무 판단을 지원하는 것이 핵심이다.

백엔드와 프론트엔드를 모두 구현한다.

주요 기능은 다음과 같다.

* Google OAuth 로그인
* 개인 플래너 및 웨딩 업체 Workspace
* 고객 관리
* 업체 관리
* 카카오맵 기반 신규 업체 검색 및 등록
* 업체 특성 관리
* 플래너별 업체 경험 및 노하우 관리
* 고객·업체·플래너 일정 관리
* 신규 고객 맞춤 업체 조합 추천
* 돌발상황 발생 시 대체 업체 탐색
* 기존 거래처로 해결할 수 없는 경우 카카오맵을 통한 신규 업체 탐색
* AI Agent 및 MCP Tool을 활용한 업무 지원
* 웨딩플래너 업무용 웹 프론트엔드

---

# 핵심 사용자

서비스의 주요 사용자는 웨딩플래너다.

개인 웨딩플래너와 여러 플래너가 소속된 웨딩 업체를 모두 지원한다.

## 개인 웨딩플래너

개인적으로 활동하는 프리랜서 웨딩플래너다.

개인 Workspace에서 다음 정보를 관리한다.

* 담당 고객
* 거래 업체
* 고객 일정
* 업체 일정
* 개인 일정
* 업체 이용 경험
* 개인적인 업체 노하우

## 웨딩 업체

여러 명의 웨딩플래너가 소속된 조직이다.

하나의 Workspace에 여러 플래너가 참여할 수 있다.

다음 정보를 관리한다.

* 소속 플래너
* 회사 고객
* 제휴 업체
* 업체 이용 이력
* 고객 일정
* 업체 일정
* 플래너별 일정
* 플래너 개인 노하우
* 회사 전체에 축적되는 업체 경험

개인 플래너와 웨딩 업체를 별도의 서비스로 분리하지 않는다.

Workspace를 기준으로 사용자를 구분한다.

```text
개인 플래너

Workspace
└── User 1명
    └── OWNER
```

```text
웨딩 업체

Workspace
├── User A
│   └── OWNER
├── User B
│   └── ADMIN
└── User C
    └── MEMBER
```

모든 주요 데이터는 Workspace에 소속된다.

---

# 인증 및 권한 관리

## Google OAuth 로그인

서비스는 OAuth 로그인만 지원한다.

현재 MVP에서는 Google OAuth 로그인만 구현한다.

이메일과 비밀번호를 직접 입력하는 일반 회원가입 및 로그인 기능은 제공하지 않는다.

Spring Security OAuth2 Client를 사용한다.

## User

`User`는 Google OAuth 로그인을 통해 생성되는 서비스 계정이다.

다음 정보를 가진다.

* OAuth 제공자
* OAuth 제공자 사용자 ID
* 이메일
* 이름
* 프로필 이미지
* 계정 상태
* 가입일

비밀번호는 저장하지 않는다.

## OAuth 제공자

현재 지원하는 OAuth 제공자는 다음과 같다.

```text
GOOGLE
```

향후 다른 OAuth 제공자를 추가할 수 있도록 확장 가능하게 설계한다.

```text
OAuthProvider
└── GOOGLE
```

## 사용자 식별

사용자는 `provider`와 `providerUserId` 조합으로 식별한다.

이메일은 변경될 수 있으므로 사용자의 유일한 식별 기준으로 사용하지 않는다.

```text
provider = GOOGLE
providerUserId = Google에서 제공하는 사용자 고유 ID
```

## 첫 로그인 처리

사용자가 처음 Google 로그인을 완료하면 서비스 내부에 `User`를 생성한다.

첫 로그인 시 개인 Workspace를 자동 생성하고 사용자를 `OWNER`로 등록한다.

```text
Google OAuth 로그인 성공
↓
provider + providerUserId로 User 조회
↓
User가 없으면 생성
↓
개인 Workspace 생성
↓
WorkspaceMember 생성
↓
사용자를 OWNER로 등록
```

기존 사용자가 로그인할 때마다 새로운 Workspace를 생성하지 않는다.

---

# Workspace

`Workspace`는 서비스의 최상위 데이터 관리 단위다.

개인 플래너와 웨딩 업체 모두 동일한 Workspace 구조를 사용한다.

Workspace는 다음 데이터를 관리한다.

* 플래너
* 고객
* 업체
* 업체 경험
* 일정
* 계약
* 상담 기록

Workspace 간 데이터는 서로 공유되지 않는다.

## 개인 플래너 Workspace

```text
Workspace
└── WorkspaceMember
    ├── User
    └── Role: OWNER
```

## 웨딩 업체 Workspace

```text
Workspace
├── WorkspaceMember
│   ├── User A
│   └── Role: OWNER
├── WorkspaceMember
│   ├── User B
│   └── Role: ADMIN
└── WorkspaceMember
    ├── User C
    └── Role: MEMBER
```

---

# WorkspaceMember

`WorkspaceMember`는 `User`와 `Workspace`의 관계를 관리한다.

다음 정보를 가진다.

* 사용자
* Workspace
* 역할
* 가입일

하나의 사용자가 여러 Workspace에 참여할 수 있도록 설계한다.

---

# WorkspaceRole

```text
OWNER
ADMIN
MEMBER
```

## OWNER

* Workspace 관리
* 플래너 초대 및 제거
* 권한 변경
* 모든 고객 관리
* 모든 업체 관리
* 모든 일정 관리
* 모든 업체 노하우 관리

## ADMIN

* 플래너 관리
* 고객 관리
* 업체 관리
* 일정 관리
* 업체 노하우 관리

## MEMBER

* 담당 고객 관리
* 업체 조회
* 개인 일정 관리
* 업체 경험 등록
* AI Agent 사용

---

# 데이터 접근 원칙

모든 주요 데이터는 Workspace에 소속된다.

다음 데이터는 Workspace 단위로 격리한다.

* 고객
* 업체
* 일정
* 계약
* 상담 기록
* 업체 경험
* 플래너 노하우

사용자는 자신이 참여하고 있는 Workspace의 데이터에만 접근할 수 있다.

잘못된 예시:

```text
findCustomerById(customerId)
```

권장 예시:

```text
findCustomerByIdAndWorkspaceId(customerId, workspaceId)
```

서비스 계층에서도 현재 로그인한 사용자의 Workspace 접근 권한을 검증한다.

Controller에서만 권한을 검증하지 않는다.

AI Agent와 MCP Tool에도 동일한 데이터 접근 원칙을 적용한다.

---

# Customer

`Customer`는 결혼을 준비하는 커플을 의미한다.

## 기본 정보

* 신랑 이름
* 신부 이름
* 연락처
* 거주 지역
* 담당 플래너

## 결혼 정보

* 결혼 예정일
* 원하는 예식 지역
* 예상 하객 수
* 총예산
* 항목별 예산

## 고객 취향

* 선호하는 결혼식 분위기
* 선호 스타일
* 중요하게 생각하는 조건
* 피하고 싶은 조건

예시:

```text
예산: 3,000만 원

하객 수: 200명

희망 지역: 강남

선호 스타일:
- 자연스러운 분위기
- 밝은 웨딩홀

중요 조건:
- 지방 하객이 많아 교통이 편리해야 함

비선호 조건:
- 어두운 웨딩홀
```

## 고객 업무 정보

* 상담 기록
* 계약 현황
* 전체 일정
* 해야 할 일
* 완료한 일

---

# Vendor

`Vendor`는 웨딩 준비에 필요한 업체를 의미한다.

## VendorCategory

```text
WEDDING_HALL
STUDIO
DRESS
MAKEUP
FLOWER
JEWELRY
HANBOK
RETURN_GIFT
PHOTO
VIDEO
```

## 업체 기본 정보

* 업체명
* 위치
* 카테고리
* 담당자
* 연락처
* 영업시간
* 제휴 여부

## 업체 특성 정보

업체 추천에 활용하기 위한 특성을 관리한다.

* 대표 분위기
* 어울리는 고객 연령대
* 어울리는 고객 취향
* 추천 웨딩 스타일
* 평균 가격대
* 강점
* 주의사항
* 잘 맞는 고객 조건
* 잘 맞지 않는 고객 조건

예시:

```text
업체명: A 드레스

대표 분위기:
- 자연스러움
- 우아함

추천 고객:
- 20대 후반 ~ 30대 초반
- 자연스러운 스타일을 선호하는 고객

평균 가격대:
- 200만 원 ~ 300만 원

강점:
- 체형 커버
- 빠른 피드백

주의사항:
- 주말 예약이 빠르게 마감됨
```

## 계약 정보

* 계약 조건
* 계약 이력
* 취소 및 변경 정책

## 업무 정보

* 과거 이용 고객
* 업체 일정
* 예약 가능 시간

---

# 카카오맵 연동 및 신규 업체 등록

서비스는 신규 업체 검색 및 등록을 위해 카카오맵을 활용한다.

주요 목적은 다음과 같다.

* 실제 업체 검색
* 신규 업체 등록
* 업체 위치 정보 조회
* 주소 및 좌표 정보 활용
* AI Agent의 신규 업체 탐색 지원

## 신규 업체 등록 흐름

사용자가 새로운 업체를 Workspace에 등록할 때 업체 정보를 모두 직접 입력하지 않는다.

카카오맵에서 실제 업체를 검색한 뒤 검색 결과를 기반으로 등록한다.

```text
신규 업체 등록 선택
↓
카카오맵에서 업체 검색
↓
검색 결과 조회
↓
등록할 업체 선택
↓
카카오맵 장소 정보 조회
↓
중복 업체 확인
↓
Vendor 기본 정보 자동 입력
↓
사용자가 marry-it 내부 정보 추가 입력
↓
Workspace Vendor 등록
```

## 카카오맵 제공 정보

* 카카오맵 장소 ID
* 업체명
* 주소
* 도로명 주소
* 카테고리
* 전화번호
* 위도
* 경도
* 카카오맵 장소 URL

## marry-it 내부 관리 정보

* 웨딩 업체 카테고리
* 가격대
* 제휴 여부
* 담당자
* 계약 조건
* 업체 특성
* 대표 분위기
* 어울리는 고객 연령대
* 어울리는 고객 취향
* 추천 웨딩 스타일
* 강점
* 주의사항
* 플래너 경험
* 플래너 노하우

외부 장소 정보와 서비스 내부 업무 정보를 명확하게 구분한다.

---

# 신규 업체 중복 등록 방지

같은 Workspace에 동일한 카카오맵 업체가 중복 등록되지 않도록 한다.

카카오맵의 장소 ID를 활용한다.

권장 식별 기준:

```text
Workspace ID
+
Kakao Place ID
```

예시:

```text
existsByWorkspaceIdAndKakaoPlaceId(
    workspaceId,
    kakaoPlaceId
)
```

동일한 카카오맵 업체라도 서로 다른 Workspace에는 각각 등록할 수 있다.

---

# 카카오맵 정보 저장 원칙

카카오맵 API 응답 전체를 데이터베이스에 저장하지 않는다.

서비스에서 필요한 정보만 저장한다.

```text
kakaoPlaceId
name
address
roadAddress
phone
latitude
longitude
placeUrl
```

외부 API 응답 DTO와 내부 도메인을 분리한다.

```text
KakaoPlaceResponse
```

```text
Vendor
```

외부 API 변경이 내부 도메인에 직접 영향을 주지 않도록 한다.

---

# VendorExperience

`VendorExperience`는 플래너가 실제 업체를 이용하며 축적한 경험과 노하우다.

업체의 객관적인 정보와 플래너의 실제 경험을 분리하여 관리한다.

예시:

```text
플래너: 카야

업체: A 플라워

경험:
- 급한 주문을 잘 받아줌
- 응답이 빠름
- 화이트톤 부케를 잘함
- 주말 예약이 빠르게 마감됨
```

여러 플래너가 같은 업체에 대해 서로 다른 경험을 등록할 수 있다.

웨딩 업체 Workspace에서는 여러 플래너의 경험이 축적되어 회사의 업무 자산이 된다.

AI Agent는 업체 추천 및 대체 업체 탐색 시 해당 경험 데이터를 적극 활용한다.

---

# 일정 관리

서비스에서는 고객, 업체, 플래너의 일정을 관리한다.

## 고객 일정

* 상담
* 웨딩홀 투어
* 드레스 피팅
* 스튜디오 촬영
* 메이크업
* 본식

## 업체 일정

* 예약 가능 시간
* 상담 일정
* 방문 일정
* 계약 일정

## 플래너 일정

* 고객 상담
* 업체 방문
* 웨딩홀 투어 동행
* 드레스 피팅 동행
* 결혼 준비 업무
* 본식 지원

일정 조율 시 다음 조건을 고려한다.

* 고객 가능 일정
* 업체 가능 일정
* 플래너 가능 일정
* 업체 위치
* 일정 간 이동 가능 시간

---

# 핵심 AI 기능

## 신규 커플 맞춤 업체 조합

신규 고객의 조건에 맞는 업체 조합을 추천한다.

예시:

```text
예산 3천만 원이고
하객은 200명 정도야.

자연스러운 분위기를 좋아하고
강남 지역을 선호해.

업체 조합 추천해줘.
```

AI Agent 동작:

```text
현재 사용자 확인
↓
현재 Workspace 확인
↓
고객 정보 조회
↓
고객 예산 및 취향 확인
↓
희망 지역 확인
↓
Workspace 기존 업체 조회
↓
플래너 경험 및 노하우 조회
↓
업체 일정 확인
↓
위치 정보 확인
↓
업체 조합 생성
```

결과는 단일 업체 추천이 아닌 여러 업체의 조합이어야 한다.

---

## 돌발상황 대체 업체 탐색

예시:

```text
서영 커플 부케 업체가 갑자기 취소했어.

결혼식까지 3일 남았는데
대체 업체 찾아줘.
```

AI Agent 동작:

```text
현재 사용자 확인
↓
현재 Workspace 확인
↓
고객 정보 조회
↓
D-Day 확인
↓
고객 취향 및 예산 확인
↓
기존 거래 업체 조회
↓
플래너 경험 조회
↓
긴급 주문 대응 경험 확인
↓
업체 일정 확인
↓
대체 업체 추천
```

기존 거래 업체를 우선적으로 탐색한다.

---

## 신규 업체 발굴

기존 거래 업체로 고객의 요구사항을 만족할 수 없는 경우 카카오맵을 활용한다.

```text
기존 거래 업체 탐색
↓
플래너 노하우 확인
↓
업체 일정 확인
↓
적절한 업체가 없는 경우
↓
카카오맵 신규 업체 탐색
```

카카오맵 검색 결과는 검증되지 않은 외부 업체 후보다.

기존 업체와 외부 업체를 구분하여 사용자에게 알려야 한다.

---

# AI Agent 역할

AI Agent는 단순한 챗봇이 아니다.

서비스 데이터를 조회하고 필요한 MCP Tool을 사용하여 실제 업무 판단을 지원한다.

주요 역할:

* 고객 정보 조회
* 고객 조건 분석
* 업체 탐색
* 플래너 경험 조회
* 일정 확인
* 업체 비교
* 위치 확인
* 업체 추천
* 대체 업체 탐색
* 신규 업체 탐색

다음 데이터를 종합적으로 활용한다.

```text
고객 정보
+
업체 정보
+
플래너 경험
+
일정
+
위치 정보
```

---

# MCP Tool 설계 방향

예시:

```text
get_customer

get_customer_preferences

get_customer_budget

get_customer_schedule

search_workspace_vendors

get_vendor

get_vendor_experiences

get_vendor_schedule

search_available_vendors

search_external_vendors

get_planner_schedule

search_kakao_places
```

MCP Tool은 지나치게 많은 책임을 가지지 않는다.

Agent가 사용자의 요청에 따라 여러 Tool을 선택하고 조합한다.

추천 판단 자체는 Agent가 여러 Tool의 결과를 종합하여 수행한다.

---

# AI Agent 및 MCP 보안

AI Agent와 MCP Tool에도 일반 API와 동일한 인증 및 권한 규칙을 적용한다.

```text
현재 로그인 사용자 확인
↓
현재 Workspace 확인
↓
Workspace 접근 권한 확인
↓
해당 Workspace의 고객 조회
↓
해당 Workspace의 업체 조회
↓
접근 가능한 플래너 경험 조회
↓
필요한 일정 및 위치 정보 조회
↓
추천 결과 생성
```

다른 Workspace의 데이터가 노출되어서는 안 된다.

---

# 프론트엔드 구현 범위

백엔드와 함께 웹 프론트엔드를 구현한다.

프론트엔드는 웨딩플래너가 고객, 업체, 일정, 업체 경험 및 AI Agent를 사용할 수 있는 업무용 웹 애플리케이션으로 구현한다.

MVP에서는 화려한 UI보다 핵심 업무 흐름을 명확하게 보여주는 것을 우선한다.

일반 예비부부용 웨딩 앱이 아니라 웨딩플래너가 사용하는 B2B 업무 도구의 형태로 구성한다.

---

# 주요 화면

## 로그인 화면

* Google OAuth 로그인 버튼
* 로그인 실패 안내
* 로그인 성공 후 기본 Workspace 이동

## 대시보드

* 오늘 일정
* 담당 고객 목록
* 결혼식이 가까운 고객
* 최근 등록 업체
* 처리해야 할 업무
* AI Agent 입력 영역

## 고객 관리 화면

* 고객 목록
* 고객 검색
* 고객 등록
* 고객 상세
* 고객 수정
* 고객 취향 관리
* 고객 예산 관리
* 고객 일정 조회
* 상담 기록
* 해야 할 일 및 완료한 일

## 업체 관리 화면

* 업체 목록
* 업체 검색
* 업체 카테고리 필터
* 업체 상세
* 업체 수정
* 업체 특성 관리
* 업체 경험 및 노하우 관리

## 카카오맵 기반 신규 업체 등록 화면

* 업체 검색 입력창
* 카카오맵 장소 검색
* 검색 결과 목록
* 업체 선택
* 카카오맵 기본 정보 자동 입력
* Workspace 내부 정보 추가 입력
* 신규 Vendor 등록

## 일정 관리 화면

* 고객 일정
* 업체 일정
* 플래너 일정
* 일정 등록
* 일정 수정
* 일정 삭제
* 일정 충돌 확인

## AI Agent 화면

* 자연어 요청 입력
* AI Agent 응답 표시
* 추천 업체 표시
* 추천 이유 표시
* 기존 Workspace 업체와 카카오맵 외부 업체 구분

예시 요청:

```text
서영 커플에게 어울리는 업체 조합 추천해줘.
```

```text
부케 업체가 갑자기 취소됐어.
대체 업체 찾아줘.
```

---

# 프론트엔드 구현 원칙

화면에서 직접 비즈니스 로직을 처리하지 않는다.

다음 로직은 백엔드에서 처리한다.

* Workspace 접근 권한
* 고객 조회 권한
* 업체 중복 등록 검증
* 예산 검증
* 일정 충돌 검증
* 업체 추천
* 대체 업체 탐색
* AI Agent Tool 호출

프론트엔드는 사용자 입력을 받고 백엔드 API를 호출하며 결과를 보여주는 역할에 집중한다.

---

# 프론트엔드 구현 우선순위

```text
1. Google OAuth 로그인

2. 대시보드

3. 고객 관리

4. 업체 관리

5. 카카오맵 기반 신규 업체 등록

6. 업체 경험 및 노하우 관리

7. AI Agent 요청 및 응답

8. 일정 관리
```

---

# 핵심 구현 원칙

## 기본 서비스의 역할

* Google OAuth 로그인
* 첫 로그인 시 User 생성
* 첫 로그인 시 개인 Workspace 생성
* WorkspaceMember 관리
* 고객 CRUD
* 업체 CRUD
* 카카오맵 기반 신규 업체 등록
* 일정 CRUD
* 업체 경험 CRUD

## AI Agent의 역할

* 고객 조건 분석
* 업체 조합 추천
* 대체 업체 탐색
* 플래너 경험 활용
* 신규 업체 탐색
* 여러 데이터와 Tool을 활용한 업무 판단

## 검증 가능한 비즈니스 규칙은 AI에 의존하지 않는다

다음 로직은 애플리케이션 코드에서 처리한다.

* Workspace 접근 권한
* 예산 범위 검증
* D-Day 계산
* 일정 충돌 검증
* 업체 카테고리 필터링
* 업체 예약 가능 여부
* 동일 Kakao Place ID 중복 등록 검증
* 데이터 유효성 검증

## 존재하지 않는 데이터를 추측하지 않는다

AI Agent는 데이터베이스 또는 외부 Tool에서 확인할 수 없는 정보를 사실처럼 생성하지 않는다.

확인할 수 없는 가격, 일정, 계약 조건, 예약 가능 여부, 업체 특성 등을 임의로 생성하지 않는다.

---

# 프로젝트 구조 및 모듈 구성

이 프로젝트는 `marry-it`이라는 Gradle 기반 Spring Boot 애플리케이션이다.

* Java 21
* Spring Boot
* Gradle
* Spring Security
* OAuth2 Client
* JUnit 5

애플리케이션 진입점:

```text
src/main/java/com/zerozero/marryit/MarryItApplication.java
```

런타임 설정 및 리소스:

```text
src/main/resources/
```

테스트:

```text
src/test/java/com/zerozero/marryit/
```

---

# 권장 패키지 구조

```text
com.zerozero.marryit
├── auth
├── workspace
├── customer
├── vendor
├── schedule
├── recommendation
├── agent
├── external
│   └── kakao
└── global
```

## auth

* Google OAuth 로그인
* 인증
* 현재 사용자 조회
* OAuth 사용자 생성 및 갱신

## workspace

* Workspace 관리
* WorkspaceMember 관리
* 권한 관리
* 데이터 접근 범위 관리

## customer

* 고객 정보
* 고객 취향
* 고객 예산
* 상담 기록
* 고객 업무 관리

## vendor

* 업체 정보
* 업체 특성
* 업체 카테고리
* 업체 제휴 여부
* 업체 경험
* 플래너 노하우

## schedule

* 고객 일정
* 업체 일정
* 플래너 일정
* 일정 충돌 검증

## recommendation

* 업체 추천 도메인 로직
* 업체 후보 필터링
* 대체 업체 후보 탐색

## agent

* AI Agent
* MCP Tool
* AI 요청 및 응답
* Tool Orchestration

## external.kakao

* 카카오맵 API 호출
* 카카오맵 인증 정보 관리
* 장소 검색
* 외부 API 요청 DTO
* 외부 API 응답 DTO
* 외부 API 예외 처리

## global

* 공통 예외
* 공통 응답
* 설정
* Validation

---

# 빌드, 테스트 및 개발 명령어

Gradle Wrapper를 사용한다.

```text
./gradlew test
```

JUnit 테스트를 실행한다.

```text
./gradlew build
```

프로젝트를 빌드한다.

```text
./gradlew bootRun
```

Spring Boot 애플리케이션을 실행한다.

```text
./gradlew clean
```

빌드 결과물을 삭제한다.

---

# 코딩 스타일 및 네이밍 규칙

Java 코드는 4칸 들여쓰기를 사용한다.

* 클래스와 Record: `PascalCase`
* 메서드, 필드, Bean: `camelCase`
* 패키지: `com.zerozero.marryit` 하위 소문자

Spring Component에서는 생성자 주입을 우선적으로 사용한다.

Lombok을 사용할 수 있지만 생성되는 동작이 코드 사용 시점에서 명확하도록 작성한다.

---

# 도메인 네이밍 규칙

좋은 예시:

```text
User
OAuthProvider
Workspace
WorkspaceMember
WorkspaceRole
Customer
Vendor
VendorCategory
VendorExperience
Schedule
KakaoPlace
VendorRecommendationService
EmergencyVendorSearchService
```

피해야 할 예시:

```text
Info
Data
Manager
Helper
Processor
Util
```

---

# 테스트 가이드라인

다음 로직을 집중적으로 테스트한다.

* 최초 OAuth 로그인 시 User 생성
* 최초 OAuth 로그인 시 Workspace 생성
* 재로그인 시 중복 Workspace 생성 방지
* Workspace 접근 권한
* 다른 Workspace 데이터 접근 차단
* 고객 CRUD
* 업체 CRUD
* 고객 취향 매칭
* 예산 범위 검증
* 업체 카테고리 필터링
* 업체 경험 조회
* 업체 추천
* 대체 업체 탐색
* 일정 충돌 검증
* 카카오맵 장소 검색 결과 변환
* 카카오맵 장소 기반 Vendor 등록
* 동일 Workspace 내 동일 Kakao Place ID 중복 등록 방지
* 서로 다른 Workspace의 동일 업체 등록 허용
* 카카오맵 API 실패 시 예외 처리
* AI Agent가 기존 업체와 외부 업체를 구분하는지 확인
* AI Agent가 존재하지 않는 정보를 생성하지 않는지 확인

변경사항을 제출하기 전에 다음 명령어를 실행한다.

```text
./gradlew test
```

---

# 보안 및 설정

Secret이나 로컬 인증 정보를 Git에 Commit하지 않는다.

다음 정보를 소스 코드에 직접 작성하지 않는다.

* Google OAuth Client ID
* Google OAuth Client Secret
* Kakao API Key
* 데이터베이스 인증 정보

환경 변수 또는 로컬 설정 파일을 사용한다.

고객 개인정보를 로그에 출력하지 않는다.

다음 정보는 로그에 직접 출력하지 않는다.

* OAuth Access Token
* OAuth Refresh Token
* 전화번호
* 상세 주소
* 계약 정보
* 상담 내용
* 기타 개인정보

AI Agent와 MCP Tool 역시 현재 사용자가 접근 권한을 가진 Workspace의 데이터만 조회하고 사용할 수 있어야 한다.
