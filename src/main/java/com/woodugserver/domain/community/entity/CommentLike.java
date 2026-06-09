// package com.woodugserver.domain.community.entity;
//
// import com.woodugserver.domain.user.entity.User;
// import jakarta.persistence.*;
// import lombok.*;
// import org.springframework.data.annotation.CreatedDate;
// import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//
// import java.time.LocalDateTime;
//
// @Entity
// @Table(name = "comment_likes",
//         uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}))
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
// @Builder
// @EntityListeners(AuditingEntityListener.class)
// public class CommentLike {
//
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "comment_id", nullable = false)
//     private Comment comment;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "user_id", nullable = false)
//     private User user;
//
//     @CreatedDate
//     @Column(nullable = false, updatable = false)
//     private LocalDateTime createdAt;
//
//     public static CommentLike of(Comment comment, User user) {
//         return CommentLike.builder()
//                 .comment(comment)
//                 .user(user)
//                 .build();
//     }
// }
