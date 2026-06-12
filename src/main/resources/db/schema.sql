-- ============================================================
-- woodug KBO 야구 서비스 DDL
-- PostgreSQL 기준
-- ============================================================

-- ============================================================
-- 구장
-- ============================================================
CREATE TABLE stadiums
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL, -- 구장 정식 명칭 (예: 잠실야구장)
    short_name VARCHAR(50),           -- 약칭 (예: 잠실)
    location   VARCHAR(200),          -- 주소
    capacity   INT,                   -- 수용 인원
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE stadiums IS '구장 정보';
COMMENT ON COLUMN stadiums.name IS '구장 정식 명칭 (예: 잠실야구장)';
COMMENT ON COLUMN stadiums.capacity IS '최대 수용 인원';

-- ============================================================
-- 구단(팀)
-- ============================================================
CREATE TABLE teams
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,            -- 팀 정식 명칭 (예: 두산 베어스)
    short_name      VARCHAR(20) NOT NULL,            -- 약칭 (예: 두산)
    code            VARCHAR(10) NOT NULL UNIQUE,     -- KBO 홈페이지 스크래핑용 팀 코드
    logo_url        VARCHAR(500),                    -- 팀 로고 이미지 URL
    founded_year    VARCHAR(4),                      -- 창단 연도
    home_stadium_id BIGINT REFERENCES stadiums (id), -- 홈 구장
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE teams IS '구단(팀) 정보';
COMMENT ON COLUMN teams.code IS 'KBO 홈페이지 내부 팀 식별 코드 (스크래핑 시 사용)';

-- ============================================================
-- 시즌
-- ============================================================
CREATE TABLE seasons
(
    id         BIGSERIAL PRIMARY KEY,
    year       VARCHAR(4)  NOT NULL UNIQUE,              -- 시즌 연도 (예: 2025)
    start_date DATE        NOT NULL,                     -- 정규시즌 개막일
    status     VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED | IN_PROGRESS | FINISHED
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE seasons IS 'KBO 시즌 정보';
COMMENT ON COLUMN seasons.year IS '시즌 연도 (예: 2025)';
COMMENT ON COLUMN seasons.status IS 'SCHEDULED: 개막 전 | IN_PROGRESS: 시즌 중 | FINISHED: 시즌 종료';

-- ============================================================
-- 선수
-- ============================================================
CREATE TABLE players
(
    id                BIGSERIAL PRIMARY KEY,
    team_id           BIGINT REFERENCES teams (id),          -- 소속 팀 (이적 시 변경)
    name              VARCHAR(50) NOT NULL,                  -- 선수 이름
    back_number       INT,                                   -- 등번호
    position          VARCHAR(30) NOT NULL,                  -- PITCHER | CATCHER | FIRST_BASE | SECOND_BASE | THIRD_BASE | SHORTSTOP | LEFT_FIELD | CENTER_FIELD | RIGHT_FIELD | DESIGNATED_HITTER
    batting_hand      VARCHAR(10),                           -- 타격 방향: LEFT | RIGHT | BOTH
    throwing_hand     VARCHAR(10),                           -- 투구 방향: LEFT | RIGHT
    birth_date        DATE,                                  -- 생년월일
    height            INT,                                   -- 키 (cm)
    weight            INT,                                   -- 몸무게 (kg)
    debut_year        INT,                                   -- 데뷔 연도
    profile_image_url VARCHAR(500),                          -- 프로필 사진 URL
    is_foreign        BOOLEAN     NOT NULL DEFAULT FALSE,    -- 외국인 선수 여부
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | INJURED | SECOND_TEAM | MILITARY | FOREIGN_TEAM | RETIRED
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE players IS '선수 정보';
COMMENT ON COLUMN players.team_id IS '현재 소속 팀. 이적 시 업데이트';
COMMENT ON COLUMN players.back_number IS '등번호. 이적/변경 시 업데이트';
COMMENT ON COLUMN players.position IS '주 포지션';
COMMENT ON COLUMN players.batting_hand IS '타격 방향 (BOTH: 스위치히터)';
COMMENT ON COLUMN players.throwing_hand IS '투구/송구 방향';
COMMENT ON COLUMN players.is_foreign IS 'true: 외국인 선수 (용병)';
COMMENT ON COLUMN players.status IS 'ACTIVE: 1군 | INJURED: 부상 | SECOND_TEAM: 2군 | MILITARY: 군복무 | FOREIGN_TEAM: 해외이적 | RETIRED: 은퇴';

CREATE INDEX idx_players_team_id ON players (team_id);
CREATE INDEX idx_players_status ON players (status);

-- ============================================================
-- 경기
-- ============================================================
CREATE TABLE games
(
    id             BIGSERIAL PRIMARY KEY,
    kbo_game_id    VARCHAR(20),                                -- KBO 홈페이지 경기 고유 ID (예: 20260328KTLG0)
    season_id      BIGINT      NOT NULL REFERENCES seasons (id),
    home_team_id   BIGINT      NOT NULL REFERENCES teams (id), -- 홈 팀
    away_team_id   BIGINT      NOT NULL REFERENCES teams (id), -- 원정 팀
    stadium_id     BIGINT      NOT NULL REFERENCES stadiums (id),
    game_date      DATE        NOT NULL,                       -- 경기 날짜 (조회 기준)
    scheduled_at   TIMESTAMP   NOT NULL,                       -- 예정 시작 시각 (우천 등으로 변경 가능)
    status         VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',   -- 경기 상태
    home_score     INT         NOT NULL DEFAULT 0,
    away_score     INT         NOT NULL DEFAULT 0,
    current_inning INT,                                        -- 현재 이닝 (진행 중일 때)
    inning_half    VARCHAR(10),                                -- TOP: 초(원정 공격) | BOTTOM: 말(홈 공격)
    started_at     TIMESTAMP,                                  -- 실제 시작 시각
    ended_at       TIMESTAMP,                                  -- 실제 종료 시각
    game_note               VARCHAR(200),            -- 경기 상태명 (예: 정상경기, 우천취소, 강풍취소)
    is_called_game          BOOL        NOT NULL DEFAULT FALSE,
    away_starting_pitcher   VARCHAR(50),             -- 원정 선발투수 이름
    home_starting_pitcher   VARCHAR(50),             -- 홈 선발투수 이름
    winning_pitcher         VARCHAR(50),             -- 승리투수
    losing_pitcher          VARCHAR(50),             -- 패전투수
    save_pitcher            VARCHAR(50),             -- 세이브
    created_at              TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE games IS 'KBO 경기 정보';
COMMENT ON COLUMN games.game_date IS '경기 날짜. scheduled_at과 별도로 날짜 기준 조회에 사용';
COMMENT ON COLUMN games.scheduled_at IS '예정 시작 시각. 우천 지연 시 업데이트';
COMMENT ON COLUMN games.status IS 'SCHEDULED: 예정 | IN_PROGRESS: 진행중 | SUSPENDED: 중단 | FINISHED: 종료 | CANCELLED: 취소/연기';
COMMENT ON COLUMN games.inning_half IS 'TOP: 초(원정팀 공격) | BOTTOM: 말(홈팀 공격)';
COMMENT ON COLUMN games.game_note IS '경기 상태명. KBO API CANCEL_SC_NM 값 (예: 정상경기, 우천취소, 강풍취소)';
COMMENT ON COLUMN games.is_called_game IS '콜드게임(Called Game) 여부. status=FINISHED이면서 정규 이닝 전에 종료된 경기 (우천콜드, 점수차콜드)';

CREATE INDEX idx_games_game_date ON games (game_date);
CREATE INDEX idx_games_status ON games (status);
CREATE INDEX idx_games_season_id ON games (season_id);
CREATE INDEX idx_games_home_team ON games (home_team_id);
CREATE INDEX idx_games_away_team ON games (away_team_id);
-- 취소되지 않은 경기만 kbo_game_id 유일성 보장 (취소 이력은 중복 허용)
CREATE UNIQUE INDEX idx_games_kbo_game_id_active ON games (kbo_game_id) WHERE status <> 'CANCELLED';

-- ============================================================
-- 이닝별 점수
-- ============================================================
CREATE TABLE game_innings
(
    id         BIGSERIAL PRIMARY KEY,
    game_id    BIGINT    NOT NULL REFERENCES games (id),
    inning     INT       NOT NULL,           -- 이닝 번호 (1~12+)
    home_score INT       NOT NULL DEFAULT 0, -- 해당 이닝 홈팀 득점
    away_score INT       NOT NULL DEFAULT 0, -- 해당 이닝 원정팀 득점
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (game_id, inning)
);

COMMENT ON TABLE game_innings IS '경기 이닝별 득점 기록';
COMMENT ON COLUMN game_innings.inning IS '이닝 번호. 연장전은 10이닝 이상';
COMMENT ON COLUMN game_innings.home_score IS '해당 이닝에서의 홈팀 득점 (누적 아님)';
COMMENT ON COLUMN game_innings.away_score IS '해당 이닝에서의 원정팀 득점 (누적 아님)';

CREATE INDEX idx_game_innings_game_id ON game_innings (game_id);

-- ============================================================
-- 타자 경기 기록
-- ============================================================
CREATE TABLE batter_game_records
(
    id              BIGSERIAL PRIMARY KEY,
    game_id         BIGINT    NOT NULL REFERENCES games (id),
    player_id       BIGINT    NOT NULL REFERENCES players (id),
    team_id         BIGINT    NOT NULL REFERENCES teams (id),
    batting_order   INT,                          -- 타순 (1~9)
    at_bats         INT       NOT NULL DEFAULT 0, -- 타수
    hits            INT       NOT NULL DEFAULT 0, -- 안타
    doubles         INT       NOT NULL DEFAULT 0, -- 2루타
    triples         INT       NOT NULL DEFAULT 0, -- 3루타
    home_runs       INT       NOT NULL DEFAULT 0, -- 홈런
    rbis            INT       NOT NULL DEFAULT 0, -- 타점
    runs            INT       NOT NULL DEFAULT 0, -- 득점
    stolen_bases    INT       NOT NULL DEFAULT 0, -- 도루
    caught_stealing INT       NOT NULL DEFAULT 0, -- 도루 실패
    walks           INT       NOT NULL DEFAULT 0, -- 볼넷
    strikeouts      INT       NOT NULL DEFAULT 0, -- 삼진
    hit_by_pitch    INT       NOT NULL DEFAULT 0, -- 사구
    sacrifice_hits  INT       NOT NULL DEFAULT 0, -- 희타 (번트)
    sacrifice_flies INT       NOT NULL DEFAULT 0, -- 희비
    double_play     INT       NOT NULL DEFAULT 0, -- 병살타
    batting_avg     NUMERIC(4, 3),                -- 해당 경기 후 타율
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE batter_game_records IS '타자 경기별 기록';
COMMENT ON COLUMN batter_game_records.batting_order IS '타순 (1~9번)';
COMMENT ON COLUMN batter_game_records.sacrifice_hits IS '희생 번트';
COMMENT ON COLUMN batter_game_records.sacrifice_flies IS '희생 플라이';
COMMENT ON COLUMN batter_game_records.batting_avg IS '경기 후 시즌 누적 타율';

CREATE INDEX idx_batter_records_game_id ON batter_game_records (game_id);
CREATE INDEX idx_batter_records_player_id ON batter_game_records (player_id);

-- ============================================================
-- 투수 경기 기록
-- ============================================================
CREATE TABLE pitcher_game_records
(
    id                BIGSERIAL PRIMARY KEY,
    game_id           BIGINT    NOT NULL REFERENCES games (id),
    player_id         BIGINT    NOT NULL REFERENCES players (id),
    team_id           BIGINT    NOT NULL REFERENCES teams (id),
    pitch_order       INT,                          -- 등판 순서 (1: 선발, 2~: 계투)
    innings_pitched   NUMERIC(4, 1),                -- 투구이닝 (KBO 표기: 6.2 = 6이닝 2아웃)
    batters_faced     INT       NOT NULL DEFAULT 0, -- 상대한 타자 수
    hits              INT       NOT NULL DEFAULT 0, -- 피안타
    home_runs         INT       NOT NULL DEFAULT 0, -- 피홈런
    earned_runs       INT       NOT NULL DEFAULT 0, -- 자책점
    runs              INT       NOT NULL DEFAULT 0, -- 실점 (비자책 포함)
    walks             INT       NOT NULL DEFAULT 0, -- 볼넷
    intentional_walks INT       NOT NULL DEFAULT 0, -- 고의사구
    strikeouts        INT       NOT NULL DEFAULT 0, -- 탈삼진
    hit_batsmen       INT       NOT NULL DEFAULT 0, -- 사구(몸에 맞는 공)
    wild_pitches      INT       NOT NULL DEFAULT 0, -- 폭투
    pitch_count       INT       NOT NULL DEFAULT 0, -- 투구수
    result            VARCHAR(20),                  -- WIN | LOSE | SAVE | HOLD | BLOWN_SAVE | NO_DECISION
    era               NUMERIC(5, 2),                -- 경기 후 시즌 누적 ERA
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE pitcher_game_records IS '투수 경기별 기록';
COMMENT ON COLUMN pitcher_game_records.pitch_order IS '등판 순서. 1: 선발, 2 이상: 계투';
COMMENT ON COLUMN pitcher_game_records.innings_pitched IS 'KBO 표기법 준수: .1 = 1아웃, .2 = 2아웃 (수학적 분수 아님)';
COMMENT ON COLUMN pitcher_game_records.earned_runs IS '자책점 (비자책 실점 제외)';
COMMENT ON COLUMN pitcher_game_records.result IS 'WIN | LOSE | SAVE | HOLD | BLOWN_SAVE | NO_DECISION';
COMMENT ON COLUMN pitcher_game_records.era IS '경기 후 시즌 누적 평균자책점';

CREATE INDEX idx_pitcher_records_game_id ON pitcher_game_records (game_id);
CREATE INDEX idx_pitcher_records_player_id ON pitcher_game_records (player_id);

-- ============================================================
-- 팀 순위
-- ============================================================
CREATE TABLE team_standings
(
    id            BIGSERIAL PRIMARY KEY,
    season_id     BIGINT    NOT NULL REFERENCES seasons (id),
    team_id       BIGINT    NOT NULL REFERENCES teams (id),
    rank          INT       NOT NULL DEFAULT 0, -- 순위
    games_played  INT       NOT NULL DEFAULT 0, -- 경기수
    wins          INT       NOT NULL DEFAULT 0, -- 승
    losses        INT       NOT NULL DEFAULT 0, -- 패
    draws         INT       NOT NULL DEFAULT 0, -- 무
    winning_pct   NUMERIC(5, 3),                -- 승률
    games_behind  NUMERIC(5, 1),                -- 게임차 (1위 기준)
    streak        VARCHAR(10),                  -- 연승/연패 표기 (예: W3, L2)
    home_wins     INT       NOT NULL DEFAULT 0,
    home_losses   INT       NOT NULL DEFAULT 0,
    home_draws    INT       NOT NULL DEFAULT 0,
    away_wins     INT       NOT NULL DEFAULT 0,
    away_losses   INT       NOT NULL DEFAULT 0,
    away_draws    INT       NOT NULL DEFAULT 0,
    last10_wins   INT       NOT NULL DEFAULT 0, -- 최근 10경기 승
    last10_losses INT       NOT NULL DEFAULT 0, -- 최근 10경기 패
    last10_draws  INT       NOT NULL DEFAULT 0, -- 최근 10경기 무
    runs_scored   INT       NOT NULL DEFAULT 0, -- 시즌 득점
    runs_allowed  INT       NOT NULL DEFAULT 0, -- 시즌 실점
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (season_id, team_id)
);

COMMENT ON TABLE team_standings IS '팀 시즌 순위 및 성적';
COMMENT ON COLUMN team_standings.games_behind IS '1위와의 게차. 1위는 0 또는 -';
COMMENT ON COLUMN team_standings.streak IS '연승/연패 표기 (예: W3 = 3연승, L2 = 2연패)';
COMMENT ON COLUMN team_standings.last10_wins IS '최근 10경기 기준 성적';

CREATE INDEX idx_standings_season_id ON team_standings (season_id);

-- ============================================================
-- 사용자
-- ============================================================
CREATE TABLE users
(
    id                BIGSERIAL PRIMARY KEY,
    email             VARCHAR(100) UNIQUE,                   -- 카카오 이메일 미동의 시 NULL 가능
    password          VARCHAR(200),                          -- bcrypt 해시. 소셜 전용 회원은 NULL
    nickname          VARCHAR(50) NOT NULL UNIQUE,
    profile_image_url VARCHAR(500),
    provider          VARCHAR(20),                           -- NULL: 일반회원 | GOOGLE | KAKAO
    provider_id       VARCHAR(100),                          -- 소셜 플랫폼 사용자 고유 ID
    favorite_team_id  BIGINT REFERENCES teams (id),          -- 응원팀 (1개만, 변경 가능)
    role              VARCHAR(20) NOT NULL DEFAULT 'USER',   -- USER | ADMIN
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | SUSPENDED | WITHDRAWN
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    withdrawn_at      TIMESTAMP,                             -- 탈퇴 처리 시각
    UNIQUE (provider, provider_id)                           -- 동일 소셜 계정 중복 가입 방지
);

COMMENT ON TABLE users IS '서비스 사용자';
COMMENT ON COLUMN users.email IS 'NULL 허용: 카카오 이메일 미동의 케이스. UNIQUE이지만 PostgreSQL은 NULL 여러 개 허용';
COMMENT ON COLUMN users.password IS 'bcrypt 해시값. 소셜 전용 회원은 NULL. 평문 저장 금지';
COMMENT ON COLUMN users.provider IS 'NULL: 일반 이메일 회원 | GOOGLE | KAKAO';
COMMENT ON COLUMN users.provider_id IS '소셜 플랫폼이 발급한 사용자 고유 ID (Google: sub, Kakao: id)';
COMMENT ON COLUMN users.favorite_team_id IS '응원팀 ID. 1개만 지정 가능, 미설정 시 NULL';
COMMENT ON COLUMN users.role IS 'USER: 일반 | ADMIN: 관리자';
COMMENT ON COLUMN users.status IS 'ACTIVE: 정상 | SUSPENDED: 정지 | WITHDRAWN: 탈퇴';
COMMENT ON COLUMN users.withdrawn_at IS '탈퇴 처리 시각. 일정 기간 후 개인정보 파기';

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);

-- ============================================================
-- 리프레시 토큰
-- ============================================================
CREATE TABLE user_refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    token      VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE user_refresh_tokens IS 'JWT 리프레시 토큰 저장소';
COMMENT ON COLUMN user_refresh_tokens.token IS 'Refresh Token 문자열';
COMMENT ON COLUMN user_refresh_tokens.expires_at IS '만료 시각. 만료된 토큰은 주기적으로 삭제';

CREATE INDEX idx_refresh_tokens_user_id ON user_refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token ON user_refresh_tokens (token);

-- ============================================================
-- 직관 기록
-- ============================================================
CREATE TABLE attendances
(
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT    NOT NULL REFERENCES users (id),
    game_id        BIGINT    NOT NULL REFERENCES games (id),
    seat_section   VARCHAR(50),                        -- 좌석 구역 (예: 1루 내야석, 외야 응원석)
    seat_row       VARCHAR(20),                        -- 열 (예: A, B, 12)
    seat_number    VARCHAR(20),                        -- 번호
    review         TEXT,                               -- 직관 후기
    rating         INT CHECK (rating BETWEEN 1 AND 5), -- 별점 1~5
    weather        VARCHAR(50),                        -- 날씨 (예: 맑음, 흐림)
    companion_type VARCHAR(20),                        -- SOLO | COUPLE | FRIENDS | FAMILY
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMP                           -- soft delete
);

COMMENT ON TABLE attendances IS '사용자 직관(현장 관람) 기록';
COMMENT ON COLUMN attendances.seat_section IS '좌석 구역명 (구장마다 구역명 상이)';
COMMENT ON COLUMN attendances.rating IS '경기/직관 만족도 별점 1~5';
COMMENT ON COLUMN attendances.companion_type IS 'SOLO: 혼자 | COUPLE: 연인 | FRIENDS: 친구 | FAMILY: 가족';
COMMENT ON COLUMN attendances.deleted_at IS 'soft delete. NULL이면 정상, 값이 있으면 삭제됨';

CREATE INDEX idx_attendances_user_id ON attendances (user_id);
CREATE INDEX idx_attendances_game_id ON attendances (game_id);

-- ============================================================
-- 게시글
-- ============================================================
CREATE TABLE posts
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id),
    team_id       BIGINT REFERENCES teams (id), -- 팀 게시판인 경우 팀 지정
    category      VARCHAR(30)  NOT NULL,        -- FREE | TEAM | GAME_RECAP | TRADE_RUMORS | ATTENDANCE
    title         VARCHAR(200) NOT NULL,
    content       TEXT         NOT NULL,
    view_count    INT          NOT NULL DEFAULT 0,
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP                     -- soft delete
);

COMMENT ON TABLE posts IS '커뮤니티 게시글';
COMMENT ON COLUMN posts.team_id IS '팀 게시판 글인 경우 팀 ID. FREE 카테고리는 NULL';
COMMENT ON COLUMN posts.category IS 'FREE: 자유 | TEAM: 팀별 | GAME_RECAP: 경기리뷰 | TRADE_RUMORS: 이적설 | ATTENDANCE: 직관후기';
COMMENT ON COLUMN posts.deleted_at IS 'soft delete. 삭제 후에도 댓글 맥락 유지를 위해 보존';

CREATE INDEX idx_posts_user_id ON posts (user_id);
CREATE INDEX idx_posts_category ON posts (category);
CREATE INDEX idx_posts_team_id ON posts (team_id);
CREATE INDEX idx_posts_created_at ON posts (created_at DESC);

-- ============================================================
-- 댓글
-- ============================================================
CREATE TABLE comments
(
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT    NOT NULL REFERENCES posts (id),
    user_id    BIGINT    NOT NULL REFERENCES users (id),
    parent_id  BIGINT REFERENCES comments (id), -- NULL: 최상위 댓글 / 값: 대댓글
    content    TEXT      NOT NULL,
    like_count INT       NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP                        -- soft delete
);

COMMENT ON TABLE comments IS '게시글 댓글 및 대댓글';
COMMENT ON COLUMN comments.parent_id IS 'NULL: 최상위 댓글. 값이 있으면 해당 댓글의 대댓글. 최대 1단계 대댓글만 허용';
COMMENT ON COLUMN comments.deleted_at IS 'soft delete. 삭제된 댓글은 "삭제된 댓글입니다" 표시용으로 row 보존';

CREATE INDEX idx_comments_post_id ON comments (post_id);
CREATE INDEX idx_comments_parent_id ON comments (parent_id);

-- ============================================================
-- 게시글 좋아요
-- ============================================================
CREATE TABLE post_likes
(
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT    NOT NULL REFERENCES posts (id),
    user_id    BIGINT    NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);

COMMENT ON TABLE post_likes IS '게시글 좋아요. 사용자당 게시글 1회만 가능 (UNIQUE 제약)';

CREATE INDEX idx_post_likes_post_id ON post_likes (post_id);

-- ============================================================
-- 댓글 좋아요
-- ============================================================
CREATE TABLE comment_likes
(
    id         BIGSERIAL PRIMARY KEY,
    comment_id BIGINT    NOT NULL REFERENCES comments (id),
    user_id    BIGINT    NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (comment_id, user_id)
);

COMMENT ON TABLE comment_likes IS '댓글 좋아요. 사용자당 댓글 1회만 가능 (UNIQUE 제약)';

CREATE INDEX idx_comment_likes_comment_id ON comment_likes (comment_id);


