// package com.woodugserver.domain.attendance.entity;
//
// import com.woodugserver.domain.game.entity.Game;
// import com.woodugserver.domain.user.entity.User;
// import com.woodugserver.global.entity.BaseEntity;
// import jakarta.persistence.*;
// import lombok.*;
//
// import java.time.LocalDateTime;
//
// @Entity
// @Table(name = "attendances",
//         indexes = @Index(name = "idx_attendances_user_id", columnList = "user_id"))
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
// @Builder
// public class Attendance extends BaseEntity {
//
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "user_id", nullable = false)
//     private User user;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "game_id", nullable = false)
//     private Game game;
//
//     @Column(length = 50)
//     private String seatSection;   // 좌석 구역
//
//     @Column(length = 20)
//     private String seatRow;       // 열
//
//     @Column(length = 20)
//     private String seatNumber;    // 번호
//
//     @Column(columnDefinition = "TEXT")
//     private String review;
//
//     @Column
//     private Integer rating;       // 1~5
//
//     @Column(length = 50)
//     private String weather;
//
//     @Enumerated(EnumType.STRING)
//     @Column(length = 20)
//     private CompanionType companionType;
//
//     private LocalDateTime deletedAt;
//
//     public void update(String seatSection, String seatRow, String seatNumber,
//                        String review, Integer rating, String weather, CompanionType companionType) {
//         this.seatSection = seatSection;
//         this.seatRow = seatRow;
//         this.seatNumber = seatNumber;
//         this.review = review;
//         this.rating = rating;
//         this.weather = weather;
//         this.companionType = companionType;
//     }
//
//     public void softDelete() {
//         this.deletedAt = LocalDateTime.now();
//     }
//
//     public boolean isDeleted() {
//         return this.deletedAt != null;
//     }
// }
