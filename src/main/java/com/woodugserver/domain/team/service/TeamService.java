package com.woodugserver.domain.team.service;

import com.woodugserver.domain.team.dto.TeamResponse;
import com.woodugserver.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<TeamResponse> findAll() {
        return teamRepository.findAll().stream()
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .map(TeamResponse::from)
                .toList();
    }
}
