package com.woodugserver.scraping.client;

import com.woodugserver.scraping.dto.KboGameDto;
import com.woodugserver.scraping.dto.KboGameListResponse;
import com.woodugserver.scraping.dto.KboScoreBoardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KboApiClient {

    private static final String GAME_LIST_URL   = "https://www.koreabaseball.com/ws/Main.asmx/GetKboGameList";
    private static final String SCORE_BOARD_URL = "https://www.koreabaseball.com/ws/Schedule.asmx/GetScoreBoardScroll";
    private static final String SR_ID = "0";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestTemplate restTemplate;

    public List<KboGameDto> fetchGames(LocalDate date) {
        String dateStr = date.format(DATE_FMT);
        try {
            KboGameListResponse response = callApi(dateStr, SR_ID);
            if (response == null || response.getGame() == null) return List.of();
            return response.getGame().stream()
                    .filter(g -> g.getGId() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("KBO API 호출 실패: date={}, error={}", dateStr, e.getMessage());
            return List.of();
        }
    }

    public Optional<KboScoreBoardResponse> fetchScoreBoard(String gameId, int seasonId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Referer", "https://www.koreabaseball.com/");

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("leId", "1");
            params.add("srId", SR_ID);
            params.add("seasonId", String.valueOf(seasonId));
            params.add("gameId", gameId);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<KboScoreBoardResponse> response =
                    restTemplate.postForEntity(SCORE_BOARD_URL, request, KboScoreBoardResponse.class);

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("KBO ScoreBoard API 호출 실패: gameId={}, error={}", gameId, e.getMessage());
            return Optional.empty();
        }
    }

    private KboGameListResponse callApi(String dateStr, String srId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Referer", "https://www.koreabaseball.com/");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("leId", "1");
        params.add("srId", srId);
        params.add("date", dateStr);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<KboGameListResponse> response =
                restTemplate.postForEntity(GAME_LIST_URL, request, KboGameListResponse.class);

        return response.getBody();
    }
}
