package com.woodugserver.scraping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class KboGameDto {

    @JsonProperty("G_ID")       private String  gId;
    @JsonProperty("G_DT")       private String  gDt;
    @JsonProperty("G_TM")       private String  gTm;
    @JsonProperty("S_NM")       private String  sNm;
    @JsonProperty("HOME_ID")    private String  homeId;
    @JsonProperty("AWAY_ID")    private String  awayId;
    @JsonProperty("B_SCORE_CN") private String  bScoreCn;   // 홈팀 득점
    @JsonProperty("T_SCORE_CN") private String  tScoreCn;   // 원정팀 득점
    @JsonProperty("GAME_STATE_SC")  private String  gameStateSc;
    @JsonProperty("CANCEL_SC_ID")   private String  cancelScId;
    @JsonProperty("CANCEL_SC_NM")   private String  cancelScNm; //
    @JsonProperty("GAME_INN_NO")    private Integer gameInnNo; // 현재 이닝
    @JsonProperty("GAME_TB_SC")     private String  gameTbSc; // 초(T), 말(B)
    @JsonProperty("SEASON_ID")      private Integer seasonId; // 몇 년도
}
