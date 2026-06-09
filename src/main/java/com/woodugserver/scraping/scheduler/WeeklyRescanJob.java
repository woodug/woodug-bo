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
        LocalDate today = LocalDate.now();
        log.info("[WeeklyRescanJob] 미래 일정 재스캔 시작: {}~", today);
        gameScrapingService.scrapeForward(today);
    }
}
