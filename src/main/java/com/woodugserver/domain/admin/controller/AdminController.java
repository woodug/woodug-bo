package com.woodugserver.domain.admin.controller;

import com.woodugserver.global.response.ApiResponse;
import com.woodugserver.scraping.service.GameScrapingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final GameScrapingService gameScrapingService;

    @PostMapping("/backfill/game-details")
    public ResponseEntity<ApiResponse<Void>> backfillGameDetails() {
        gameScrapingService.backfillFinishedGameDetails();
        return ResponseEntity.ok(ApiResponse.ok("백필을 시작했습니다. 서버 로그에서 진행상황을 확인하세요."));
    }
}
