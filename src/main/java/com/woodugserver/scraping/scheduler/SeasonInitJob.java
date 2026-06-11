package com.woodugserver.scraping.scheduler;

import com.woodugserver.domain.game.repository.GameRepository;
import com.woodugserver.domain.season.entity.Season;
import com.woodugserver.domain.season.repository.SeasonRepository;
import com.woodugserver.domain.standing.repository.TeamStandingRepository;
import com.woodugserver.scraping.service.GameScrapingService;
import com.woodugserver.scraping.service.StandingsScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeasonInitJob implements ApplicationRunner {

    private final SeasonRepository seasonRepository;
    private final GameRepository gameRepository;
    private final TeamStandingRepository standingRepository;
    private final GameScrapingService gameScrapingService;
    private final StandingsScrapingService standingsScrapingService;

    @Override
    public void run(ApplicationArguments args) {
        String year = String.valueOf(LocalDate.now().getYear());
        Season season = seasonRepository.findByYear(year).orElse(null);

        if (season == null) {
            log.info("[SeasonInitJob] {}년 시즌 데이터 없음, 전체 일정 스크랩 시작", year);
            gameScrapingService.initSeasonScrape(year);
            return;
        }

        log.info("[SeasonInitJob] {}년 시즌 데이터 존재, 누락 데이터 확인", year);

        // 이닝 백필: FINISHED 경기 중 이닝 미적재 경기가 있으면 실행
        List<Long> missingInnings = gameRepository.findFinishedGameIdsWithoutInnings();
        if (!missingInnings.isEmpty()) {
            log.info("[SeasonInitJob] 이닝 미적재 경기 {}개, 백필 시작", missingInnings.size());
            gameScrapingService.backfillFinishedGameDetails();
        }

        // 순위 초기 적재: 순위 데이터가 없으면 즉시 스크랩
        if (standingRepository.findBySeasonOrderByRankAsc(season).isEmpty()) {
            log.info("[SeasonInitJob] 순위 데이터 없음, 초기 스크랩 시작");
            standingsScrapingService.forceSync();
        }
    }
}
