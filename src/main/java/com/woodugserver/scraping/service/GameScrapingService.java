package com.woodugserver.scraping.service;

import com.woodugserver.domain.game.entity.*;
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
    private final TeamRepository teamRepository;
    private final StadiumRepository stadiumRepository;
    private final SeasonRepository seasonRepository;

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
    // 미래 일정 재스캔 (WeeklyRescanJob에서 호출)
    // ---------------------------------------------------------------

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
        int created = 0, updated = 0;
        for (KboGameDto dto : dtos) {
            try {
                boolean isNew = upsertGame(dto, season);
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                log.error("[Scraping] 게임 처리 실패: gId={}, error={}", dto.getGId(), e.getMessage(), e);
            }
        }
        log.info("[Scraping] {} 동기화 완료: 신규={}, 갱신={}", date, created, updated);
    }

    private boolean upsertGame(KboGameDto dto, Season season) {
        Optional<Game> existing = gameRepository.findByKboGameId(dto.getGId());
        GameStatus newStatus = resolveStatus(dto);

        if (existing.isEmpty()) {
            Game game = buildNewGame(dto, season, newStatus);
            if (game != null) {
                gameRepository.save(game);
                return true;
            }
            return false;
        }

        updateGame(existing.get(), dto, newStatus);
        return false;
    }

    private Game buildNewGame(KboGameDto dto, Season season, GameStatus status) {
        Team homeTeam = teamByCode.get(dto.getHomeId());
        Team awayTeam = teamByCode.get(dto.getAwayId());
        if (homeTeam == null || awayTeam == null) {
            log.warn("[Scraping] 팀 코드 매핑 실패: gId={}, home={}, away={}",
                dto.getGId(), dto.getHomeId(), dto.getAwayId());
            return null;
        }

        Stadium stadium = resolveStadium(dto.getSNm());
        LocalDateTime scheduledAt = parseScheduledAt(dto.getGDt(), dto.getGTm());
        if (stadium == null || scheduledAt == null) {
            log.warn("[Scraping] 경기장 또는 일정 파싱 실패: gId={}, sNm={}, gDt={}, gTm={}",
                dto.getGId(), dto.getSNm(), dto.getGDt(), dto.getGTm());
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
            .cancelReason(dto.getCancelScNm())
            .isCalledGame(calledGame)
            .build();
    }

    private void updateGame(Game game, KboGameDto dto, GameStatus newStatus) {
        GameStatus prev = game.getStatus();
        Integer inning = dto.getGameInnNo();
        InningHalf half = resolveInningHalf(dto.getGameTbSc());

        if (prev == GameStatus.SCHEDULED && newStatus == GameStatus.IN_PROGRESS) {
            game.start();
        } else if (prev == GameStatus.IN_PROGRESS && newStatus == GameStatus.SUSPENDED) {
            game.suspend();
        } else if (prev == GameStatus.SUSPENDED && newStatus == GameStatus.IN_PROGRESS) {
            game.resume();
        } else if (newStatus == GameStatus.FINISHED && prev != GameStatus.FINISHED) {
            if (isCalledByInning(inning)) {
                game.finishAsColdGame();
            } else {
                game.finish();
            }
        } else if (newStatus == GameStatus.CANCELLED && prev != GameStatus.CANCELLED) {
            game.cancel(dto.getCancelScNm());
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
    }

    private GameStatus resolveStatus(KboGameDto dto) {
        String cancelId = dto.getCancelScId();
        if (cancelId != null && !cancelId.isBlank() && !"0".equals(cancelId)) {
            String nm = dto.getCancelScNm();
            return GameStatus.CANCELLED;
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
