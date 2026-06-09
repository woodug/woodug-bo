package com.woodugserver.scraping.scheduler;

import com.woodugserver.scraping.service.GameScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyRescanJob {

    private final GameScrapingService gameScrapingService;

    @Scheduled(cron = "${scraping.kbo.schedule.weekly-rescan:0 0 3 * * MON}")
    public void run() {
        String year = String.valueOf(LocalDate.now().getYear());
        log.info("[WeeklyRescanJob] {}년 전체 시즌 재스캔 시작", year);
        // 시작일부터 전체 재스캔 — 초기 스크랩에서 놓친 구간도 보완
        gameScrapingService.rescanFullSeason(year);
    }
}
