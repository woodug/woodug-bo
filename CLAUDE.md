# woodug-server

KBO 야구 정보 서비스의 백엔드 서버. 실시간 점수, 팀/선수 정보, 직관 기록, 커뮤니티 기능 제공.

## 기술 스택

- **언어/프레임워크**: Java 17, Spring Boot 3.5.x
- **데이터베이스**: PostgreSQL
- **ORM**: Spring Data JPA + QueryDSL 5.1.0 (Jakarta)
- **인증**: Spring Security + JWT (jjwt 0.12.6)
- **스크래핑**: Playwright (Java) + Jsoup 보조
- **빌드**: Gradle

## 패키지 구조

```
com.woodugserver
├── domain          # 도메인별 패키지 (team, player, game, user, community ...)
│   └── {domain}
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       └── dto
├── global
│   ├── config      # Security, QueryDSL, Playwright 등 설정
│   ├── exception   # 공통 예외 처리
│   ├── jwt         # JWT 유틸리티
│   └── response    # 공통 응답 형식
└── scraping        # KBO 스크래핑 스케줄러 및 로직
```

## 핵심 설정

### QueryDSL
- Q클래스 생성 경로: `src/main/generated/`
- `.gitignore`에 포함 — 빌드 후 생성되므로 커밋하지 않음
- `gradle compileJava` 실행 시 Q클래스 생성됨

### JWT
- Access Token: 30분 (`jwt.access-token-expiration`)
- Refresh Token: 7일 (`jwt.refresh-token-expiration`)
- Secret은 환경변수 `JWT_SECRET`으로 주입

### 스크래핑
- 실시간 점수: 5분 간격 (`scraping.kbo.schedule.score`)
- 일정/순위 등: 매일 오전 6시 (`scraping.kbo.schedule.daily`)
- Playwright를 주 도구로 사용 (JS 렌더링 필요), 정적 페이지는 Jsoup 보조 활용

### 프로파일
- `dev`: `ddl-auto=create-drop`, SQL 로그 출력
- `prod`: `ddl-auto=validate`, 커넥션 풀 20

## 커밋 규칙

Conventional Commits 형식을 따른다.

```
<type>(<scope>): <subject>
```

**type**
| type | 용도 |
|---|---|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변화 없는 코드 개선 |
| `chore` | 빌드, 의존성, 설정 변경 |
| `docs` | 문서/주석 |
| `test` | 테스트 코드 |
| `style` | 포맷 등 코드 의미 변화 없는 수정 |

**scope**: `auth`, `user`, `game`, `scraping`, `global` 등 패키지 단위

**subject**: 한글 사용, 명령형으로 작성 (예: "구현", "수정", "추가")

예시:
```
feat(auth): 이메일 회원가입/로그인 구현
chore(build): Redis 의존성 추가
fix(scraping): 시즌 시작일 파싱 오류 수정
refactor(auth): refresh token 저장소 PostgreSQL → Redis 교체
```

## 개발 규칙

- DTO ↔ Entity 변환은 서비스 레이어에서 수행
- 페이징은 QueryDSL + `Pageable` 조합
- 공통 응답 형식: `ApiResponse<T>` 래퍼 사용
- 예외는 `GlobalExceptionHandler`(@RestControllerAdvice)에서 중앙 처리
- 환경변수: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`

## 로컬 개발 환경

```bash
# 프로파일 지정 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# QueryDSL Q클래스 생성
./gradlew compileJava
```