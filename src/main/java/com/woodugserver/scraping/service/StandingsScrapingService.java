package com.woodugserver.scraping.service;

import com.woodugserver.domain.game.entity.GameStatus;
import com.woodugserver.domain.game.repository.GameRepository;
import com.woodugserver.domain.season.entity.Season;
import com.woodugserver.domain.season.repository.SeasonRepository;
import com.woodugserver.domain.standing.entity.TeamStanding;
import com.woodugserver.domain.standing.repository.TeamStandingRepository;
import com.woodugserver.domain.team.entity.Team;
import com.woodugserver.domain.team.repository.TeamRepository;
import com.woodugserver.scraping.client.KboApiClient;
import com.woodugserver.scraping.dto.KboStandingsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandingsScrapingService {

    private final KboApiClient kboApiClient;
    private final TeamStandingRepository standingRepository;
    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final SeasonRepository seasonRepository;

    public void syncStandings(LocalDate date) {
        String year = String.valueOf(date.getYear());
        Season season = seasonRepository.findByYear(year).orElse(null);
        if (season == null) return;

        // 오늘 종료된 경기가 없으면 갱신 불필요
        if (!gameRepository.existsByGameDateAndStatusIn(date, List.of(GameStatus.FINISHED))) return;

        // 아직 진행 중인 경기가 있으면 대기
        if (gameRepository.existsByGameDateAndStatusIn(date,
                List.of(GameStatus.IN_PROGRESS, GameStatus.SUSPENDED))) return;

        // 이미 오늘 갱신됐으면 스킵
        if (standingRepository.existsBySeasonIdAndUpdatedAtAfter(season.getId(), date.atStartOfDay())) return;

        List<KboStandingsDto> dtos = kboApiClient.fetchStandings();
        if (dtos.isEmpty()) {
            log.warn("[Standings] 스크랩 결과 없음");
            return;
        }

        Map<String, Team> teamByShortName = teamRepository.findAll().stream()
                .collect(Collectors.toMap(Team::getShortName, t -> t));

        // 검증: DB 경기 수 < 스크랩 경기 수인 팀이 있으면 KBO가 아직 미갱신
        for (KboStandingsDto dto : dtos) {
            Team team = teamByShortName.get(dto.getTeamName());
            if (team == null) {
                log.warn("[Standings] 팀 매핑 실패: '{}'", dto.getTeamName());
                continue;
            }
            int dbCount = gameRepository.countFinishedByTeamAndSeason(team.getId(), season.getId());
            if (dbCount > dto.getGamesPlayed()) {
                log.debug("[Standings] 아직 미갱신: team={}, db={}, scraped={}",
                        dto.getTeamName(), dbCount, dto.getGamesPlayed());
                return;
            }
        }

        int saved = 0;
        for (KboStandingsDto dto : dtos) {
            Team team = teamByShortName.get(dto.getTeamName());
            if (team == null) continue;

            TeamStanding standing = standingRepository.findBySeasonAndTeam(season, team)
                    .orElseGet(() -> standingRepository.save(
                            TeamStanding.builder().season(season).team(team).build()));
            standing.update(dto);
            standingRepository.save(standing);
            saved++;
        }

        log.info("[Standings] {} 순위표 갱신 완료 ({}팀)", date, saved);
    }
}
