package com.woodugserver.scraping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woodugserver.domain.game.entity.*;
import com.woodugserver.domain.game.repository.GameInningRepository;
import com.woodugserver.domain.game.repository.GameRepository;
import com.woodugserver.domain.season.entity.Season;
import com.woodugserver.domain.season.entity.SeasonStatus;
import com.woodugserver.domain.season.repository.SeasonRepository;
import com.woodugserver.domain.team.entity.Stadium;
import com.woodugserver.domain.team.entity.Team;
import com.woodugserver.domain.team.repository.StadiumRepository;
import com.woodugserver.domain.team.repository.TeamRepository;
import com.woodugserver.scraping.client.KboApiClient;
import com.woodugserver.scraping.dto.KboGameDto;
import com.woodugserver.scraping.dto.KboScoreBoardResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameScrapingService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int EMPTY_DAY_THRESHOLD = 10;
    private static final long SCRAPE_DELAY_MS = 500;

    private static final Map<String, Long> STADIUM_KEYWORD_ID = new LinkedHashMap<>();

    static {
        // 1군 홈구장
        STADIUM_KEYWORD_ID.put("잠실", 1L);
        STADIUM_KEYWORD_ID.put("고척", 2L);
        STADIUM_KEYWORD_ID.put("문학", 3L);
        STADIUM_KEYWORD_ID.put("수원", 4L);
        STADIUM_KEYWORD_ID.put("창원", 5L);
        STADIUM_KEYWORD_ID.put("대구", 6L);
        STADIUM_KEYWORD_ID.put("대전", 7L);
        STADIUM_KEYWORD_ID.put("사직", 8L);
        STADIUM_KEYWORD_ID.put("광주", 9L);
        // 1군 대체구장
        STADIUM_KEYWORD_ID.put("포항", 10L);
        STADIUM_KEYWORD_ID.put("마산", 11L);
        STADIUM_KEYWORD_ID.put("청주", 12L);
        STADIUM_KEYWORD_ID.put("울산", 13L);
    }

    private final KboApiClient kboApiClient;
    private final GameRepository gameRepository;
    private final GameInningRepository gameInningRepository;
    private final TeamRepository teamRepository;
    private final StadiumRepository stadiumRepository;
    private final SeasonRepository seasonRepository;
    private final ObjectMapper objectMapper;

    private Map<String, Team> teamByCode;
    private Map<Long, Stadium> stadiumById;

    @PostConstruct
    public void init() {
        teamByCode = new HashMap<>();
        teamRepository.findAll().forEach(t -> teamByCode.put(t.getCode(), t));

        stadiumById = new HashMap<>();
        stadiumRepository.findAll().forEach(s -> stadiumById.put(s.getId(), s));

        log.info("GameScrapingService 초기화 완료: 팀 {}개, 경기장 {}개 캐시됨",
            teamByCode.size(), stadiumById.size());
    }

    // ---------------------------------------------------------------
    // 당일 동기화 (DailyScheduleJob, GameSyncJob에서 호출)
    // ---------------------------------------------------------------

    @Transactional
    public void syncGames(LocalDate date) {
        String year = String.valueOf(date.getYear());
        Season season = seasonRepository.findByYear(year).orElse(null);
        if (season == null) {
            log.warn("[Scraping] {}년 시즌 데이터 없음, syncGames 생략 (date={})", year, date);
            return;
        }

        List<KboGameDto> dtos = kboApiClient.fetchGames(date);
        if (dtos.isEmpty()) {
            log.debug("[Scraping] {} 조회 결과 없음", date);
            return;
        }

        processDayGames(dtos, season, date);
    }

    // ---------------------------------------------------------------
    // 전체 시즌 최초 적재 (SeasonInitJob, DailyScheduleJob에서 호출)
    // ---------------------------------------------------------------

    @Async
    public void initSeasonScrape(String year) {
        int yearInt = Integer.parseInt(year);
        LocalDate startDate = findSeasonStartDate(yearInt);
        log.info("[Scraping] {}년 시즌 시작일: {}", year, startDate);

        Season season = createSeason(year, startDate);
        doScrapeForward(startDate, season);
    }

    // ---------------------------------------------------------------
    // 전체 시즌 재스캔 (WeeklyRescanJob에서 호출 — 시작일부터 전체 재검토)
    // ---------------------------------------------------------------

    @Async
    public void rescanFullSeason(String year) {
        Season season = seasonRepository.findByYear(year).orElse(null);
        if (season == null) {
            log.warn("[Scraping] {}년 시즌 데이터 없음, rescanFullSeason 생략", year);
            return;
        }
        log.info("[Scraping] {}년 전체 시즌 재스캔 시작 (from={})", year, season.getStartDate());
        doScrapeForward(season.getStartDate(), season);
    }

    @Async
    public void scrapeForward(LocalDate from) {
        String year = String.valueOf(from.getYear());
        Season season = seasonRepository.findByYear(year).orElse(null);
        if (season == null) {
            log.warn("[Scraping] {}년 시즌 데이터 없음, scrapeForward 생략", year);
            return;
        }
        doScrapeForward(from, season);
    }

    // ---------------------------------------------------------------
    // 종료 경기 상세 동기화 (실제 시작/종료 시각 + 이닝별 점수)
    // ---------------------------------------------------------------

    @Transactional
    public void syncFinishedGameDetails(LocalDate date) {
        List<Game> finishedGames = gameRepository.findByGameDateAndStatus(date, GameStatus.FINISHED);
        for (Game game : finishedGames) {
            if (gameInningRepository.existsByGameId(game.getId())) continue;

            int seasonId = Integer.parseInt(game.getSeason().getYear());
            kboApiClient.fetchScoreBoard(game.getKboGameId(), seasonId).ifPresent(sb -> {
                game.setActualTimes(parseTime(game.getGameDate(), sb.getStartTm()),
                                    parseTime(game.getGameDate(), sb.getEndTm()));

                if (sb.getTable2() != null) {
                    List<GameInning> innings = parseInnings(game, sb.getTable2());
                    if (!innings.isEmpty()) {
                        gameInningRepository.saveAll(innings);
                        log.info("[Scraping] 이닝 적재: gId={}, {}이닝", game.getKboGameId(), innings.size());
                    }
                }
            });
        }
    }

    // ---------------------------------------------------------------
    // private
    // ---------------------------------------------------------------

    private LocalDate findSeasonStartDate(int year) {
        LocalDate cursor = LocalDate.of(year, 3, 1);
        LocalDate limit = LocalDate.of(year, 5, 1);
        while (cursor.isBefore(limit)) {
            try {
                if (!kboApiClient.fetchGames(cursor).isEmpty()) {
                    return cursor;
                }
                cursor = cursor.plusDays(1);
                Thread.sleep(SCRAPE_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[Scraping] findSeasonStartDate 인터럽트됨, cursor={}", cursor);
                return cursor;
            }
        }

        log.warn("[Scraping] {}년 시즌 시작일 미발견, 3/1 대체", year);
        return LocalDate.of(year, 3, 1);
    }

    private void doScrapeForward(LocalDate from, Season season) {
        LocalDate cursor = from;
        int consecutiveEmpty = 0;

        while (consecutiveEmpty < EMPTY_DAY_THRESHOLD) {
            try {
                List<KboGameDto> dtos = kboApiClient.fetchGames(cursor);
                if (dtos.isEmpty()) {
                    consecutiveEmpty++;
                } else {
                    consecutiveEmpty = 0;
                    processDayGames(dtos, season, cursor);
                }
                cursor = cursor.plusDays(1);
                Thread.sleep(SCRAPE_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[Scraping] doScrapeForward 인터럽트됨, cursor={}", cursor);
                return;
            } catch (Exception e) {
                log.error("[Scraping] {} 처리 실패: {}", cursor, e.getMessage(), e);
                cursor = cursor.plusDays(1);
            }
        }
        log.info("[Scraping] {} 이후 스크랩 완료, 마지막 경기 확인일: {}",
            from, cursor.minusDays(EMPTY_DAY_THRESHOLD));
    }

    private void processDayGames(List<KboGameDto> dtos, Season season, LocalDate date) {
        int created = 0, statusChanged = 0;
        for (KboGameDto dto : dtos) {
            try {
                int result = upsertGame(dto, season); // 1=신규, 2=상태변경, 0=점수만갱신/-1=스킵
                if (result == 1) created++;
                else if (result == 2) statusChanged++;
            } catch (Exception e) {
                log.error("[Scraping] 게임 처리 실패: gId={}, error={}", dto.getGId(), e.getMessage(), e);
            }
        }
        if (created > 0 || statusChanged > 0) {
            log.info("[Scraping] {} 동기화: 신규={}, 상태변경={}", date, created, statusChanged);
        }
    }

    // 반환값: 1=신규생성, 2=상태변경, 0=점수만갱신, -1=스킵
    private int upsertGame(KboGameDto dto, Season season) {
        GameStatus newStatus = resolveStatus(dto);

        // 취소되지 않은 기존 경기 → 점수/상태 업데이트
        Optional<Game> activeGame = gameRepository.findByKboGameIdAndStatusNot(dto.getGId(), GameStatus.CANCELED);
        if (activeGame.isPresent()) {
            boolean changed = updateGame(activeGame.get(), dto, newStatus);
            return changed ? 2 : 0;
        }

        LocalDateTime scheduledAt = parseScheduledAt(dto.getGDt(), dto.getGTm());
        if (scheduledAt == null) {
            return -1;
        }
        LocalDate gameDate = scheduledAt.toLocalDate();

        // 같은 날짜에 이미 취소 이력이 있으면 스킵 — 재스캔 시 이력 덮어쓰기 방지
        if (gameRepository.existsByKboGameIdAndGameDate(dto.getGId(), gameDate)) {
            return -1;
        }

        // 완전히 새 경기이거나 다른 날짜로 재편성된 경기 → 새 row 삽입
        Game game = buildNewGame(dto, season, newStatus, scheduledAt);
        if (game != null) {
            gameRepository.save(game);
            return 1;
        }
        return -1;
    }

    private Game buildNewGame(KboGameDto dto, Season season, GameStatus status, LocalDateTime scheduledAt) {
        Team homeTeam = teamByCode.get(dto.getHomeId());
        Team awayTeam = teamByCode.get(dto.getAwayId());
        if (homeTeam == null || awayTeam == null) {
            log.warn("[Scraping] 팀 코드 매핑 실패: gId={}, home={}, away={}",
                dto.getGId(), dto.getHomeId(), dto.getAwayId());
            return null;
        }

        Stadium stadium = resolveStadium(dto.getSNm());
        if (stadium == null) {
            log.warn("[Scraping] 경기장 매핑 실패: gId={}, sNm={}", dto.getGId(), dto.getSNm());
            return null;
        }

        Integer inning = dto.getGameInnNo() == null ? 1 : dto.getGameInnNo();
        boolean calledGame = status == GameStatus.FINISHED && isCalledByInning(inning);

        return Game.builder()
            .kboGameId(dto.getGId())
            .season(season)
            .homeTeam(homeTeam)
            .awayTeam(awayTeam)
            .stadium(stadium)
            .gameDate(scheduledAt.toLocalDate())
            .scheduledAt(scheduledAt)
            .status(status)
            .homeScore(parseScore(dto.getBScoreCn()))
            .awayScore(parseScore(dto.getTScoreCn()))
            .currentInning(inning)
            .inningHalf(resolveInningHalf(dto.getGameTbSc()))
            .gameNote(trim(dto.getCancelScNm()))
            .isCalledGame(calledGame)
            .awayStartingPitcher(trim(dto.getTPitPNm()))
            .homeStartingPitcher(trim(dto.getBPitPNm()))
            .winningPitcher(trim(dto.getWPitPNm()))
            .losingPitcher(trim(dto.getLPitPNm()))
            .savePitcher(trim(dto.getSvPitPNm()))
            .build();
    }

    // true=상태 전환 발생, false=점수만 변경
    private boolean updateGame(Game game, KboGameDto dto, GameStatus newStatus) {
        GameStatus prev = game.getStatus();
        Integer inning = dto.getGameInnNo();
        InningHalf half = resolveInningHalf(dto.getGameTbSc());
        boolean statusTransitioned = false;

        if (prev == GameStatus.SCHEDULED && newStatus == GameStatus.IN_PROGRESS) {
            game.start();
            log.info("[Scraping] 경기 시작: gId={} ({} vs {})", game.getKboGameId(),
                game.getAwayTeam().getName(), game.getHomeTeam().getName());
            statusTransitioned = true;
        } else if (prev == GameStatus.IN_PROGRESS && newStatus == GameStatus.SUSPENDED) {
            game.suspend();
            log.info("[Scraping] 경기 중단: gId={}", game.getKboGameId());
            statusTransitioned = true;
        } else if (prev == GameStatus.SUSPENDED && newStatus == GameStatus.IN_PROGRESS) {
            game.resume();
            log.info("[Scraping] 경기 재개: gId={}", game.getKboGameId());
            statusTransitioned = true;
        } else if (newStatus == GameStatus.FINISHED && prev != GameStatus.FINISHED) {
            if (isCalledByInning(inning)) {
                game.finishAsColdGame();
                log.info("[Scraping] 콜드게임 종료: gId={} ({}이닝)", game.getKboGameId(), inning);
            } else {
                game.finish();
                log.info("[Scraping] 경기 종료: gId={} ({}-{})",
                    game.getKboGameId(), parseScore(dto.getTScoreCn()), parseScore(dto.getBScoreCn()));
            }
            statusTransitioned = true;
        } else if (newStatus == GameStatus.CANCELED && prev != GameStatus.CANCELED) {
            game.cancel(dto.getCancelScNm());
            log.info("[Scraping] 경기 취소: gId={}, 사유={}", game.getKboGameId(), dto.getCancelScNm());
            statusTransitioned = true;
        }

        if (newStatus == GameStatus.IN_PROGRESS
            || newStatus == GameStatus.SUSPENDED
            || newStatus == GameStatus.FINISHED) {
            game.updateScore(
                parseScore(dto.getBScoreCn()),
                parseScore(dto.getTScoreCn()),
                inning != null ? inning : 0,
                half);
        }

        game.updatePitchers(
            trim(dto.getTPitPNm()),
            trim(dto.getBPitPNm()),
            trim(dto.getWPitPNm()),
            trim(dto.getLPitPNm()),
            trim(dto.getSvPitPNm()));

        return statusTransitioned;
    }

    private GameStatus resolveStatus(KboGameDto dto) {
        String cancelId = dto.getCancelScId();
        if (cancelId != null && !cancelId.isBlank() && !"0".equals(cancelId)) {
            String nm = dto.getCancelScNm();
            return GameStatus.CANCELED;
        }
        return switch (Optional.ofNullable(dto.getGameStateSc()).orElse("0")) {
            case "1" -> GameStatus.SCHEDULED;
            case "2" -> GameStatus.IN_PROGRESS;
            case "3" -> GameStatus.FINISHED;
            default -> GameStatus.SUSPENDED;
        };
    }

    private boolean isCalledByInning(Integer inning) {
        return inning != null && inning >= 5 && inning < 9;
    }

    private Stadium resolveStadium(String sNm) {
        if (sNm == null) {
            return null;
        }
        for (Map.Entry<String, Long> entry : STADIUM_KEYWORD_ID.entrySet()) {
            if (sNm.contains(entry.getKey())) {
                return stadiumById.get(entry.getValue());
            }
        }
        log.warn("[Scraping] 경기장 매핑 실패: sNm={}", sNm);
        return null;
    }

    private LocalDateTime parseScheduledAt(String gDt, String gTm) {
        try {
            if (gDt == null || gTm == null || gDt.isBlank() || gTm.isBlank()) {
                return null;
            }
            LocalDate date = LocalDate.parse(gDt, DATE_FMT);
            LocalTime time = LocalTime.parse(gTm.trim(), TIME_FMT);
            return LocalDateTime.of(date, time);
        } catch (Exception e) {
            log.warn("[Scraping] 시간 파싱 실패: gDt={}, gTm={}", gDt, gTm);
            return null;
        }
    }

    private int parseScore(String score) {
        if (score == null || score.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(score.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String trim(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private LocalDateTime parseTime(LocalDate date, String timeTm) {
        try {
            if (timeTm == null || timeTm.isBlank()) return null;
            return LocalDateTime.of(date, LocalTime.parse(timeTm.trim(), TIME_FMT));
        } catch (Exception e) {
            return null;
        }
    }

    private List<GameInning> parseInnings(Game game, String table2Json) {
        try {
            JsonNode rows = objectMapper.readTree(table2Json).path("rows");
            if (rows.size() < 2) return List.of();

            JsonNode awayRow = rows.get(0).path("row"); // T(원정)
            JsonNode homeRow = rows.get(1).path("row"); // B(홈)

            List<GameInning> innings = new ArrayList<>();
            int count = Math.min(awayRow.size(), homeRow.size());
            for (int i = 0; i < count; i++) {
                String awayText = awayRow.get(i).path("Text").asText("-");
                String homeText = homeRow.get(i).path("Text").asText("-");
                if ("-".equals(awayText) && "-".equals(homeText)) break; // 미진행 이닝

                int awayScore = "-".equals(awayText) ? 0 : Integer.parseInt(awayText);
                int homeScore = "-".equals(homeText) ? 0 : Integer.parseInt(homeText);
                innings.add(GameInning.of(game, i + 1, homeScore, awayScore));
            }
            return innings;
        } catch (Exception e) {
            log.warn("[Scraping] 이닝 파싱 실패: gId={}, error={}", game.getKboGameId(), e.getMessage());
            return List.of();
        }
    }

    private InningHalf resolveInningHalf(String gameTbSc) {
        if (gameTbSc == null) {
            return InningHalf.TOP;
        }
        return switch (gameTbSc.trim()) {
            case "T" -> InningHalf.TOP;
            case "B" -> InningHalf.BOTTOM;
            default -> InningHalf.TOP;
        };
    }

    private Season createSeason(String year, LocalDate startDate) {
        log.info("[Scraping] {}년 시즌 생성, 시작일: {}", year, startDate);
        return seasonRepository.save(Season.builder()
            .year(year)
            .startDate(startDate)
            .status(SeasonStatus.IN_PROGRESS)
            .build());
    }
}
