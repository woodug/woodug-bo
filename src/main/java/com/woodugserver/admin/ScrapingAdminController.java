package com.woodugserver.admin;

import com.woodugserver.global.response.ApiResponse;
import com.woodugserver.scraping.scheduler.GameWindowChecker;
import com.woodugserver.scraping.service.GameScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 스크래핑 수동 실행 API (개발·운영 편의용)
 * TODO: Spring Security 설정 후 ADMIN 권한으로 보호
 */
@Slf4j
@RestController
@RequestMapping("/admin/scraping")
@RequiredArgsConstructor
public class ScrapingAdminController {

    private final GameScrapingService gameScrapingService;
    private final GameWindowChecker gameWindowChecker;

    /**
     * 특정 날짜 경기 수동 동기화
     * POST /admin/scraping/sync?date=2026-06-09
     * POST /admin/scraping/sync          (date 생략 시 오늘)
     */
    @PostMapping("/sync")
    public ApiResponse<Void> sync(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate target = (date != null) ? date : LocalDate.now();
        log.info("[Admin] 수동 동기화 요청: {}", target);
        gameScrapingService.syncGames(target);
        return ApiResponse.ok(target + " 동기화 완료");
    }

    /**
     * 날짜 범위 일괄 동기화 (과거 데이터 보정용)
     * POST /admin/scraping/sync/range?from=2026-06-01&to=2026-06-09
     */
    @PostMapping("/sync/range")
    public ApiResponse<Map<String, Object>> syncRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from.isAfter(to)) {
            return ApiResponse.ok("from이 to보다 늦습니다", Map.of("from", from, "to", to));
        }

        log.info("[Admin] 범위 동기화 요청: {} ~ {}", from, to);
        int count = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            try {
                gameScrapingService.syncGames(d);
                count++;
            } catch (Exception e) {
                log.error("[Admin] {} 동기화 실패: {}", d, e.getMessage());
            }
        }

        return ApiResponse.ok("범위 동기화 완료",
                Map.of("from", from, "to", to, "processedDays", count));
    }

    /**
     * 현재 스케줄러 윈도우 상태 확인
     * GET /admin/scraping/window
     */
    @GetMapping("/window")
    public ApiResponse<Map<String, Object>> windowStatus() {
        return ApiResponse.ok(Map.of(
                "shouldSync", gameWindowChecker.shouldSync(),
                "currentTime", java.time.LocalTime.now().toString(),
                "today", java.time.LocalDate.now().toString()
        ));
    }
}
