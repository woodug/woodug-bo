package com.woodugserver.domain.team.entity;

import com.woodugserver.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String shortName;

    // KBO 홈페이지 스크래핑 시 팀 식별에 사용
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(length = 500)
    private String logoUrl;

    private Integer foundedYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_stadium_id")
    private Stadium homeStadium;

    public void updateHomeStadium(Stadium stadium) {
        this.homeStadium = stadium;
    }
}
