// package com.woodugserver.domain.community.entity;
//
// import com.woodugserver.domain.user.entity.User;
// import com.woodugserver.global.entity.BaseEntity;
// import jakarta.persistence.*;
// import lombok.*;
//
// import java.time.LocalDateTime;
//
// @Entity
// @Table(name = "comments",
//         indexes = {
//                 @Index(name = "idx_comments_post_id", columnList = "post_id"),
//                 @Index(name = "idx_comments_parent_id", columnList = "parent_id")
//         })
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
// @Builder
// public class Comment extends BaseEntity {
//
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "post_id", nullable = false)
//     private Post post;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "user_id", nullable = false)
//     private User user;
//
//     // null이면 최상위 댓글, 값이 있으면 대댓글
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "parent_id")
//     private Comment parent;
//
//     @Column(nullable = false, columnDefinition = "TEXT")
//     private String content;
//
//     @Builder.Default
//     private Integer likeCount = 0;
//
//     private LocalDateTime deletedAt;
//
//     public void update(String content) {
//         this.content = content;
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
//     public void softDelete() {
//         this.deletedAt = LocalDateTime.now();
//     }
//
//     public boolean isDeleted() {
//         return this.deletedAt != null;
//     }
//
//     public boolean isReply() {
//         return this.parent != null;
//     }
// }
