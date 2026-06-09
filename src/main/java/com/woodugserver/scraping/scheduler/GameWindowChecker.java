package com.woodugserver.scraping.scheduler;

import com.woodugserver.domain.game.entity.GameStatus;
import com.woodugserver.domain.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GameWindowChecker {

    private static final int WINDOW_LEAD_HOURS = 2;

    private static final List<GameStatus> ACTIVE_STATUSES = List.of(
            GameStatus.SCHEDULED,
            GameStatus.IN_PROGRESS,
            GameStatus.SUSPENDED
    );

    private final GameRepository gameRepository;

    public boolean shouldSync() {
        LocalDate today = LocalDate.now();
        Optional<LocalDateTime> earliest = gameRepository.findEarliestScheduledAt(today, ACTIVE_STATUSES);
        if (earliest.isEmpty()) return false;

        LocalTime windowStart = earliest.get().toLocalTime().minusHours(WINDOW_LEAD_HOURS);
        return !LocalTime.now().isBefore(windowStart);
    }
}
