package com.woodugserver.scraping.client;

import com.woodugserver.scraping.dto.KboGameDto;
import com.woodugserver.scraping.dto.KboGameListResponse;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class KboApiClient {

    private static final String URL = "https://www.koreabaseball.com/ws/Main.asmx/GetKboGameList";
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
                restTemplate.postForEntity(URL, request, KboGameListResponse.class);

        return response.getBody();
    }
}
