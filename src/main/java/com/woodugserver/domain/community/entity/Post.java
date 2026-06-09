// package com.woodugserver.domain.community.entity;
//
// import com.woodugserver.domain.team.entity.Team;
// import com.woodugserver.domain.user.entity.User;
// import com.woodugserver.global.entity.BaseEntity;
// import jakarta.persistence.*;
// import lombok.*;
//
// import java.time.LocalDateTime;
//
// @Entity
// @Table(name = "posts",
//         indexes = {
//                 @Index(name = "idx_posts_user_id", columnList = "user_id"),
//                 @Index(name = "idx_posts_category", columnList = "category"),
//                 @Index(name = "idx_posts_team_id", columnList = "team_id")
//         })
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
// @Builder
// public class Post extends BaseEntity {
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
//     @JoinColumn(name = "team_id")
//     private Team team;
//
//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false, length = 30)
//     private PostCategory category;
//
//     @Column(nullable = false, length = 200)
//     private String title;
//
//     @Column(nullable = false, columnDefinition = "TEXT")
//     private String content;
//
//     @Builder.Default
//     private Integer viewCount = 0;
//
//     @Builder.Default
//     private Integer likeCount = 0;
//
//     @Builder.Default
//     private Integer commentCount = 0;
//
//     private LocalDateTime deletedAt;
//
//     public void update(String title, String content) {
//         this.title = title;
//         this.content = content;
//     }
//
//     public void incrementViewCount() {
//         this.viewCount++;
//     }
//
//     public void incrementLikeCount() {
//         this.likeCount++;
//     }
//
//     public void decrementLikeCount() {
//         if (this.likeCount > 0) this.likeCount--;
//     }
//
//     public void incrementCommentCount() {
//         this.commentCount++;
//     }
//
//     public void decrementCommentCount() {
//         if (this.commentCount > 0) this.commentCount--;
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
