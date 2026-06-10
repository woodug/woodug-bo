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

    // 투수 정보 (T=원정, B=홈)
    @JsonProperty("T_PIT_P_NM")  private String tPitPNm;  // 원정 선발투수
    @JsonProperty("B_PIT_P_NM")  private String bPitPNm;  // 홈 선발투수
    @JsonProperty("W_PIT_P_NM")  private String wPitPNm;  // 승리투수
    @JsonProperty("L_PIT_P_NM")  private String lPitPNm;  // 패전투수
    @JsonProperty("SV_PIT_P_NM") private String svPitPNm; // 세이브
}
