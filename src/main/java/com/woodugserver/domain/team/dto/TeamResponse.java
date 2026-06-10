package com.woodugserver.domain.team.dto;

import com.woodugserver.domain.team.entity.Team;

public record TeamResponse(
        Long id,
        String name,
        String shortName,
        String logoUrl
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getShortName(), team.getLogoUrl());
    }
}
