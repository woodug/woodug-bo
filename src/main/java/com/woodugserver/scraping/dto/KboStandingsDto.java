package com.woodugserver.scraping.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class KboStandingsDto {
    private String teamName;
    private int rank;
    private int gamesPlayed;
    private int wins;
    private int losses;
    private int draws;
    private BigDecimal winningPct;
    private BigDecimal gamesBehind;
    private String streak;
    private int homeWins;
    private int homeLosses;
    private int homeDraws;
    private int awayWins;
    private int awayLosses;
    private int awayDraws;
    private int last10Wins;
    private int last10Losses;
    private int last10Draws;
}
