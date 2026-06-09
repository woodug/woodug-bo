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
// @Table(name = "pitcher_game_records",
//         indexes = @Index(name = "idx_pitcher_records_game_id", columnList = "game_id"))
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
// @Builder
// public class PitcherGameRecord extends BaseEntity {
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
//     private Integer pitchOrder;                           // 등판 순서 (1=선발)
//
//     // KBO 표기법: 6.2 = 6이닝 2아웃 (수학적 분수 아님)
//     @Column(precision = 4, scale = 1)
//     private BigDecimal inningsPitched;
//
//     @Builder.Default private Integer battersFaced = 0;   // 상대 타자
//     @Builder.Default private Integer hits = 0;           // 피안타
//     @Builder.Default private Integer homeRuns = 0;       // 피홈런
//     @Builder.Default private Integer earnedRuns = 0;     // 자책점
//     @Builder.Default private Integer runs = 0;           // 실점
//     @Builder.Default private Integer walks = 0;          // 볼넷
//     @Builder.Default private Integer intentionalWalks = 0; // 고의4구
//     @Builder.Default private Integer strikeouts = 0;     // 탈삼진
//     @Builder.Default private Integer hitBatsmen = 0;     // 사구
//     @Builder.Default private Integer wildPitches = 0;    // 폭투
//     @Builder.Default private Integer pitchCount = 0;     // 투구수
//
//     @Enumerated(EnumType.STRING)
//     @Column(length = 20)
//     private PitcherResult result;
//
//     @Column(precision = 5, scale = 2)
//     private BigDecimal era;
// }
