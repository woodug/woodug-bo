package com.woodugserver.scraping.scheduler;

import com.woodugserver.domain.season.repository.SeasonRepository;
import com.woodugserver.scraping.service.GameScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyScheduleJob {

    private final GameScrapingService gameScrapingService;
    private final SeasonRepository seasonRepository;

    @Scheduled(cron = "${scraping.kbo.schedule.daily:0 0 6 * * *}")
    public void run() {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());

        if (seasonRepository.findByYear(year).isEmpty()) {
            log.info("[DailyScheduleJob] {}년 시즌 데이터 없음, 전체 일정 스크랩 시작", year);
            gameScrapingService.initSeasonScrape(year);
            return;
        }

        log.info("[DailyScheduleJob] {} 당일 일정 수집 시작", today);
        try {
            gameScrapingService.syncGames(today);
        } catch (Exception e) {
            log.error("[DailyScheduleJob] 실패: {}", e.getMessage(), e);
        }
    }
}
