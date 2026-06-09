package com.woodugserver.domain.game.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_innings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "inning"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class GameInning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private Integer inning;

    @Builder.Default
    private Integer homeScore = 0;

    @Builder.Default
    private Integer awayScore = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static GameInning of(Game game, int inning, int homeScore, int awayScore) {
        return GameInning.builder()
                .game(game)
                .inning(inning)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .build();
    }
}
