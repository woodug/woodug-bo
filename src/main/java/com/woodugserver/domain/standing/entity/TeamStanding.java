package com.woodugserver.domain.standing.entity;

import com.woodugserver.domain.season.entity.Season;
import com.woodugserver.domain.team.entity.Team;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_standings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"season_id", "team_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TeamStanding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Builder.Default private Integer rank = 0;
    @Builder.Default private Integer gamesPlayed = 0;
    @Builder.Default private Integer wins = 0;
    @Builder.Default private Integer losses = 0;
    @Builder.Default private Integer draws = 0;

    @Column(precision = 5, scale = 3)
    private BigDecimal winningPct;

    @Column(precision = 5, scale = 1)
    private BigDecimal gamesBehind;

    // 연승/연패 표기 (예: W3, L2)
    @Column(length = 10)
    private String streak;

    @Builder.Default private Integer homeWins = 0;
    @Builder.Default private Integer homeLosses = 0;
    @Builder.Default private Integer homeDraws = 0;
    @Builder.Default private Integer awayWins = 0;
    @Builder.Default private Integer awayLosses = 0;
    @Builder.Default private Integer awayDraws = 0;

    @Builder.Default private Integer last10Wins = 0;
    @Builder.Default private Integer last10Losses = 0;
    @Builder.Default private Integer last10Draws = 0;

    @Builder.Default private Integer runsScored = 0;
    @Builder.Default private Integer runsAllowed = 0;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void update(int rank, int wins, int losses, int draws,
                       BigDecimal winningPct, BigDecimal gamesBehind, String streak) {
        this.rank = rank;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.gamesPlayed = wins + losses + draws;
        this.winningPct = winningPct;
        this.gamesBehind = gamesBehind;
        this.streak = streak;
    }
}
