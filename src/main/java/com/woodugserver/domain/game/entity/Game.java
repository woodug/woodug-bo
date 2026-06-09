package com.woodugserver.domain.game.entity;

import com.woodugserver.domain.season.entity.Season;
import com.woodugserver.domain.team.entity.Stadium;
import com.woodugserver.domain.team.entity.Team;
import com.woodugserver.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "games",
        indexes = {
                @Index(name = "idx_games_game_date", columnList = "game_date"),
                @Index(name = "idx_games_status", columnList = "status"),
                @Index(name = "idx_games_season_id", columnList = "season_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Game extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String kboGameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    @Column(nullable = false)
    private LocalDate gameDate;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GameStatus status = GameStatus.SCHEDULED;

    @Builder.Default
    private Integer homeScore = 0;

    @Builder.Default
    private Integer awayScore = 0;

    private Integer currentInning;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private InningHalf inningHalf;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Column(length = 200)
    private String cancelReason;

    @Builder.Default
    private Boolean isCalledGame = false;

    public void start() {
        this.status = GameStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void updateScore(int homeScore, int awayScore, int inning, InningHalf half) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.currentInning = inning;
        this.inningHalf = half;
    }

    public void suspend() {
        this.status = GameStatus.SUSPENDED;
    }

    public void resume() {
        this.status = GameStatus.IN_PROGRESS;
    }

    public void finish() {
        this.status = GameStatus.FINISHED;
        this.endedAt = LocalDateTime.now();
    }

    public void finishAsColdGame() {
        this.status = GameStatus.FINISHED;
        this.isCalledGame = true;
        this.endedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        this.status = GameStatus.CANCELLED;
        this.cancelReason = reason;
    }

    public void reschedule(LocalDateTime newScheduledAt) {
        this.status = GameStatus.SCHEDULED;
        this.scheduledAt = newScheduledAt;
    }
}
