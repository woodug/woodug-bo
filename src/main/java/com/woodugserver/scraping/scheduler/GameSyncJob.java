package com.woodugserver.scraping.scheduler;

import com.woodugserver.scraping.service.GameScrapingService;
import com.woodugserver.scraping.service.StandingsScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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

    @Scheduled(fixedDelayString = "${scraping.kbo.schedule.sync-delay-ms:30000}")
    public void run() {
        LocalDate today = LocalDate.now();

        // 순위 동기화: 경기 윈도우와 무관하게 체크 (내부에서 조건 판단)
        try {
            standingsScrapingService.syncStandings(today);
        } catch (Exception e) {
            log.error("[GameSyncJob] 순위 동기화 오류: {}", e.getMessage(), e);
        }

        if (!gameWindowChecker.shouldSync()) return;

        log.debug("[GameSyncJob] {} 실시간 동기화 실행", today);
        try {
            gameScrapingService.syncGames(today);
            gameScrapingService.syncFinishedGameDetails(today);
        } catch (Exception e) {
            log.error("[GameSyncJob] 실패: {}", e.getMessage(), e);
        }
    }
}
