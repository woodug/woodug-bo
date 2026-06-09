package com.woodugserver.scraping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class KboGameListResponse {

    @JsonProperty("game")
    private List<KboGameDto> game;
}
