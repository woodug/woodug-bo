package com.woodugserver.scraping.scheduler;

import com.woodugserver.domain.season.repository.SeasonRepository;
import com.woodugserver.scraping.service.GameScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeasonInitJob implements ApplicationRunner {

    private final SeasonRepository seasonRepository;
    private final GameScrapingService gameScrapingService;

    @Override
    public void run(ApplicationArguments args) {
        String year = String.valueOf(LocalDate.now().getYear());
        if (seasonRepository.findByYear(year).isPresent()) {
            log.info("[SeasonInitJob] {}년 시즌 데이터 존재, 스크랩 생략", year);
            return;
        }
        log.info("[SeasonInitJob] {}년 시즌 데이터 없음, 전체 일정 스크랩 시작", year);
        gameScrapingService.initSeasonScrape(year);
    }
}
