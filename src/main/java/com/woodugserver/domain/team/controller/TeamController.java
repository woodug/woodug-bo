package com.woodugserver.domain.team.controller;

import com.woodugserver.domain.team.dto.TeamResponse;
import com.woodugserver.domain.team.service.TeamService;
import com.woodugserver.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeams() {
        return ResponseEntity.ok(ApiResponse.ok(teamService.findAll()));
    }
}
