package com.woodugserver.domain.game.repository;

import com.woodugserver.domain.game.entity.GameInning;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameInningRepository extends JpaRepository<GameInning, Long> {

    boolean existsByGameId(Long gameId);
}
