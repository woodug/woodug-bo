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
// @Table(name = "post_likes",
//         uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
// @Builder
// @EntityListeners(AuditingEntityListener.class)
// public class PostLike {
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
//     @CreatedDate
//     @Column(nullable = false, updatable = false)
//     private LocalDateTime createdAt;
//
//     public static PostLike of(Post post, User user) {
//         return PostLike.builder()
//                 .post(post)
//                 .user(user)
//                 .build();
//     }
// }
