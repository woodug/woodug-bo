package com.woodugserver.scraping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class KboScoreBoardResponse {

    @JsonProperty("G_ID")      private String  gId;
    @JsonProperty("G_DT")      private String  gDt;
    @JsonProperty("SEASON_ID") private Integer seasonId;
    @JsonProperty("START_TM")  private String  startTm;  // "14:00"
    @JsonProperty("END_TM")    private String  endTm;    // "16:39"
    @JsonProperty("table2")    private String  table2;   // 이닝별 점수 JSON 문자열
}
