package com.woodugserver.domain.standing.repository;

import com.woodugserver.domain.season.entity.Season;
import com.woodugserver.domain.standing.entity.TeamStanding;
import com.woodugserver.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeamStandingRepository extends JpaRepository<TeamStanding, Long> {

    Optional<TeamStanding> findBySeasonAndTeam(Season season, Team team);

    List<TeamStanding> findBySeasonOrderByRankAsc(Season season);

    boolean existsBySeasonIdAndUpdatedAtAfter(Long seasonId, LocalDateTime after);
}
