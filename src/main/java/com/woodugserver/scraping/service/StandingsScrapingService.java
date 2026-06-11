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

    // 오늘 마지막으로 동기화한 시점의 FINISHED 경기 수 (새 경기가 끝날 때만 재스크랩)
    private volatile LocalDate lastSyncDate = null;
    private volatile int lastSyncedFinishedCount = -1;

    /**
     * 오늘 새로 종료된 경기가 있을 때 순위표를 스크랩해서 저장한다.
     * IN_PROGRESS/SUSPENDED 경기가 있어도 진행 — 끝난 경기 기준 순위 즉시 반영.
     */
    public void syncStandings(LocalDate date) {
        String year = String.valueOf(date.getYear());
        Season season = seasonRepository.findByYear(year).orElse(null);
        if (season == null) return;

        int todayFinished = gameRepository.countByGameDateAndStatus(date, GameStatus.FINISHED);
        if (todayFinished == 0) return;

        // 날짜가 바뀌면 카운터 리셋
        if (!date.equals(lastSyncDate)) {
            lastSyncDate = date;
            lastSyncedFinishedCount = -1;
        }

        // 마지막 동기화 이후 새로 종료된 경기가 없으면 스킵
        if (todayFinished <= lastSyncedFinishedCount) return;

        doSync(season, date, todayFinished);
    }

    /**
     * 날짜/경기 조건 없이 현재 KBO 순위표를 즉시 적재한다 (서버 초기 기동용).
     */
    public void forceSync() {
        String year = String.valueOf(LocalDate.now().getYear());
        Season season = seasonRepository.findByYear(year).orElse(null);
        if (season == null) return;

        List<KboStandingsDto> dtos = kboApiClient.fetchStandings();
        if (dtos.isEmpty()) {
            log.warn("[Standings] 초기 스크랩 실패: 결과 없음");
            return;
        }

        Map<String, Team> teamMap = buildTeamMap();
        int saved = saveAll(dtos, season, teamMap);
        log.info("[Standings] 초기 순위 적재 완료 ({}팀)", saved);
    }

    // ---------------------------------------------------------------
    // private
    // ---------------------------------------------------------------

    private void doSync(Season season, LocalDate date, int todayFinished) {
        List<KboStandingsDto> dtos = kboApiClient.fetchStandings();
        if (dtos.isEmpty()) {
            log.warn("[Standings] 스크랩 결과 없음");
            return;
        }

        Map<String, Team> teamMap = buildTeamMap();

        // 검증: DB 시즌 누적 경기수 > 스크랩 경기수인 팀이 있으면 KBO 아직 미갱신
        for (KboStandingsDto dto : dtos) {
            Team team = teamMap.get(dto.getTeamName());
            if (team == null) continue;
            int dbCount = gameRepository.countFinishedByTeamAndSeason(team.getId(), season.getId());
            if (dbCount > dto.getGamesPlayed()) {
                log.debug("[Standings] KBO 미갱신: team={}, db={}, scraped={}",
                        dto.getTeamName(), dbCount, dto.getGamesPlayed());
                return;
            }
        }

        int saved = saveAll(dtos, season, teamMap);
        lastSyncedFinishedCount = todayFinished;
        log.info("[Standings] {} 순위표 갱신 완료 ({}팀)", date, saved);
    }

    private int saveAll(List<KboStandingsDto> dtos, Season season, Map<String, Team> teamMap) {
        int saved = 0;
        for (KboStandingsDto dto : dtos) {
            Team team = teamMap.get(dto.getTeamName());
            if (team == null) {
                log.warn("[Standings] 팀 매핑 실패: '{}'", dto.getTeamName());
                continue;
            }
            TeamStanding standing = standingRepository.findBySeasonAndTeam(season, team)
                    .orElseGet(() -> standingRepository.save(
                            TeamStanding.builder().season(season).team(team).build()));
            standing.update(dto);
            standingRepository.save(standing);
            saved++;
        }
        return saved;
    }

    private Map<String, Team> buildTeamMap() {
        return teamRepository.findAll().stream()
                .collect(Collectors.toMap(Team::getShortName, t -> t));
    }
}
