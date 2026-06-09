// package com.woodugserver.domain.player.entity;
//
// import com.woodugserver.domain.team.entity.Team;
// import com.woodugserver.global.entity.BaseEntity;
// import jakarta.persistence.*;
// import lombok.*;
//
// import java.time.LocalDate;
//
// @Entity
// @Table(name = "players")
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
// @Builder
// public class Player extends BaseEntity {
//
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "team_id")
//     private Team team;
//
//     @Column(nullable = false, length = 50)
//     private String name;
//
//     private Integer backNumber;
//
//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false, length = 30)
//     private Position position;
//
//     @Enumerated(EnumType.STRING)
//     @Column(length = 10)
//     private Hand battingHand;
//
//     @Enumerated(EnumType.STRING)
//     @Column(length = 10)
//     private Hand throwingHand;
//
//     private LocalDate birthDate;
//
//     private Integer height;
//
//     private Integer weight;
//
//     private Integer debutYear;
//
//     @Column(length = 500)
//     private String profileImageUrl;
//
//     @Column(nullable = false)
//     @Builder.Default
//     private Boolean isForeign = false;
//
//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false, length = 20)
//     @Builder.Default
//     private PlayerStatus status = PlayerStatus.ACTIVE;
//
//     public void transfer(Team newTeam) {
//         this.team = newTeam;
//     }
//
//     public void updateStatus(PlayerStatus status) {
//         this.status = status;
//     }
//
//     public void updateBackNumber(Integer backNumber) {
//         this.backNumber = backNumber;
//     }
// }
