package com.woodugserver.scraping.scheduler;

import com.woodugserver.domain.game.entity.GameStatus;
import com.woodugserver.domain.game.repository.GameRepository;
import com.woodugserver.scraping.service.GameScrapingService;
import com.woodugserver.scraping.service.StandingsScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 30초마다 실행, 단 경기 시간대(15:00~자정)에만 실제 동기화 수행
 * 경기 시작 전 취소 공지 확인(14:00~17:30)도 이 잡이 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameSyncJob {

    private final GameScrapingService gameScrapingService;
    private final StandingsScrapingService standingsScrapingService;
    private final GameWindowChecker gameWindowChecker;
    private final GameRepository gameRepository;

    @Scheduled(fixedDelayString = "${scraping.kbo.schedule.sync-delay-ms:30000}")
    public void run() {
        LocalDate today = LocalDate.now();

        // 순위 동기화: 경기 윈도우와 무관하게 체크 (내부에서 조건 판단)
        try {
            standingsScrapingService.syncStandings(today);
        } catch (Exception e) {
            log.error("[GameSyncJob] 순위 동기화 오류: {}", e.getMessage(), e);
        }

        if (!gameWindowChecker.shouldSync()) {
            syncTodayFinishedIfNeeded(today);    // 이닝/투수 미적재 재시도
            syncTomorrowPitchersIfNeeded(today); // 내일 선발투수 동기화
            return;
        }

        log.debug("[GameSyncJob] {} 실시간 동기화 실행", today);
        try {
            gameScrapingService.syncGames(today);
            gameScrapingService.syncFinishedGameDetails(today);
        } catch (Exception e) {
            log.error("[GameSyncJob] 실패: {}", e.getMessage(), e);
        }
    }

    private void syncTodayFinishedIfNeeded(LocalDate today) {
        boolean missingInnings  = gameRepository.countFinishedWithoutInningsByDate(today) > 0;
        boolean missingPitchers = gameRepository.countFinishedWithMissingWinnerByDate(today) > 0;
        if (!missingInnings && !missingPitchers) return;

        log.debug("[GameSyncJob] {} 종료 경기 재동기화 (이닝미적재={}, 투수미적재={})", today, missingInnings, missingPitchers);
        try {
            gameScrapingService.syncGames(today);
            gameScrapingService.syncFinishedGameDetails(today);
        } catch (Exception e) {
            log.error("[GameSyncJob] 오늘 종료 경기 재동기화 오류: {}", e.getMessage(), e);
        }
    }

    private void syncTomorrowPitchersIfNeeded(LocalDate today) {
        // 오늘 FINISHED 경기가 없으면 스킵 (경기 없는 날 또는 경기 시작 전)
        if (!gameRepository.existsByGameDateAndStatusIn(today, List.of(GameStatus.FINISHED))) return;

        LocalDate tomorrow = today.plusDays(1);
        if (gameRepository.countScheduledWithMissingPitchers(tomorrow) == 0) return;

        log.debug("[GameSyncJob] {} 선발투수 미등록 경기 있음, 내일 일정 동기화", tomorrow);
        try {
            gameScrapingService.syncGames(tomorrow);
        } catch (Exception e) {
            log.error("[GameSyncJob] 내일 선발투수 동기화 오류: {}", e.getMessage(), e);
        }
    }
}
