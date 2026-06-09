// package com.woodugserver.domain.game.entity;
//
// import com.woodugserver.domain.player.entity.Player;
// import com.woodugserver.domain.team.entity.Team;
// import com.woodugserver.global.entity.BaseEntity;
// import jakarta.persistence.*;
// import lombok.*;
//
// import java.math.BigDecimal;
//
// @Entity
// @Table(name = "batter_game_records",
//         indexes = @Index(name = "idx_batter_records_game_id", columnList = "game_id"))
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
// @Builder
// public class BatterGameRecord extends BaseEntity {
//
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "game_id", nullable = false)
//     private Game game;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "player_id", nullable = false)
//     private Player player;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "team_id", nullable = false)
//     private Team team;
//
//     private Integer battingOrder;
//
//     @Builder.Default private Integer atBats = 0;          // 타수
//     @Builder.Default private Integer hits = 0;            // 안타
//     @Builder.Default private Integer doubles = 0;         // 2루타
//     @Builder.Default private Integer triples = 0;         // 3루타
//     @Builder.Default private Integer homeRuns = 0;        // 홈런
//     @Builder.Default private Integer rbis = 0;            // 타점
//     @Builder.Default private Integer runs = 0;            // 득점
//     @Builder.Default private Integer stolenBases = 0;     // 도루
//     @Builder.Default private Integer caughtStealing = 0;  // 도루 실패
//     @Builder.Default private Integer walks = 0;           // 볼넷
//     @Builder.Default private Integer strikeouts = 0;      // 삼진
//     @Builder.Default private Integer hitByPitch = 0;      // 사구
//     @Builder.Default private Integer sacrificeHits = 0;   // 희타
//     @Builder.Default private Integer sacrificeFlies = 0;  // 희비
//     @Builder.Default private Integer doublePlay = 0;      // 병살타
//
//     @Column(precision = 4, scale = 3)
//     private BigDecimal battingAvg;                        // 타율
// }
