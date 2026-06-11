package com.woodugserver.domain.game.repository;

import com.woodugserver.domain.game.entity.Game;
import com.woodugserver.domain.game.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByKboGameId(String kboGameId);

    Optional<Game> findByKboGameIdAndStatusNot(String kboGameId, GameStatus status);

    boolean existsByKboGameIdAndGameDate(String kboGameId, LocalDate gameDate);

    List<Game> findByGameDate(LocalDate gameDate);

    List<Game> findByGameDateAndStatus(LocalDate gameDate, GameStatus status);

    @Query("SELECT g.id FROM Game g WHERE g.status = 'FINISHED' AND NOT EXISTS (SELECT gi FROM GameInning gi WHERE gi.game = g)")
    List<Long> findFinishedGameIdsWithoutInnings();

    List<Game> findByGameDateAndStatusIn(LocalDate gameDate, List<GameStatus> statuses);

    boolean existsByGameDateAndStatusIn(LocalDate gameDate, List<GameStatus> statuses);

    @Query("SELECT MIN(g.scheduledAt) FROM Game g WHERE g.gameDate = :date AND g.status IN :statuses")
    Optional<LocalDateTime> findEarliestScheduledAt(@Param("date") LocalDate date, @Param("statuses") List<GameStatus> statuses);

    @Query("SELECT COUNT(g) FROM Game g WHERE g.season.id = :seasonId AND g.status = 'FINISHED' AND (g.homeTeam.id = :teamId OR g.awayTeam.id = :teamId)")
    int countFinishedByTeamAndSeason(@Param("teamId") Long teamId, @Param("seasonId") Long seasonId);

    int countByGameDateAndStatus(LocalDate gameDate, GameStatus status);
}
