package com.woodugserver.scraping.client;

import com.woodugserver.scraping.dto.KboGameDto;
import com.woodugserver.scraping.dto.KboGameListResponse;
import com.woodugserver.scraping.dto.KboScoreBoardResponse;
import com.woodugserver.scraping.dto.KboStandingsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KboApiClient {

    private static final String GAME_LIST_URL   = "https://www.koreabaseball.com/ws/Main.asmx/GetKboGameList";
    private static final String SCORE_BOARD_URL = "https://www.koreabaseball.com/ws/Schedule.asmx/GetScoreBoardScroll";
    private static final String TEAM_RANK_URL   = "https://www.koreabaseball.com/Record/TeamRank/TeamRank.aspx";
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

    public List<KboStandingsDto> fetchStandings() {
        try {
            Document doc = Jsoup.connect(TEAM_RANK_URL)
                    .header("Referer", "https://www.koreabaseball.com/")
                    .timeout(10_000)
                    .get();

            Element table = findStandingsTable(doc);
            if (table == null) {
                log.warn("[Standings] 순위표 테이블을 찾을 수 없음");
                return List.of();
            }

            List<KboStandingsDto> result = new ArrayList<>();
            for (Element row : table.select("tbody tr")) {
                Elements cells = row.select("td");
                if (cells.size() < 12) continue;
                KboStandingsDto dto = parseStandingsRow(cells);
                if (dto != null) result.add(dto);
            }
            return result;
        } catch (Exception e) {
            log.warn("[Standings] 순위표 스크랩 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private Element findStandingsTable(Document doc) {
        for (Element table : doc.select("table")) {
            String headerText = table.select("thead th, thead td").eachText()
                    .stream().reduce("", String::concat);
            if (headerText.isBlank()) {
                headerText = table.select("tr:first-child th, tr:first-child td").eachText()
                        .stream().reduce("", String::concat);
            }
            if (headerText.contains("순위") && headerText.contains("승률") && headerText.contains("홈")) {
                return table;
            }
        }
        return null;
    }

    private KboStandingsDto parseStandingsRow(Elements cells) {
        try {
            int rank          = parseIntCell(cells.get(0).text());
            String teamName   = cells.get(1).text().trim();
            int gamesPlayed   = parseIntCell(cells.get(2).text());
            int wins          = parseIntCell(cells.get(3).text());
            int losses        = parseIntCell(cells.get(4).text());
            int draws         = parseIntCell(cells.get(5).text());
            BigDecimal pct    = parsePct(cells.get(6).text());
            BigDecimal gb     = parseGamesBehind(cells.get(7).text());
            int[] last10      = parseRecord(cells.get(8).text());
            String streak     = parseStreak(cells.get(9).text());
            int[] home        = parseRecord(cells.get(10).text());
            int[] away        = parseRecord(cells.get(11).text());

            if (teamName.isBlank()) return null;

            return KboStandingsDto.builder()
                    .rank(rank).teamName(teamName).gamesPlayed(gamesPlayed)
                    .wins(wins).losses(losses).draws(draws)
                    .winningPct(pct).gamesBehind(gb).streak(streak)
                    .last10Wins(last10[0]).last10Draws(last10[1]).last10Losses(last10[2])
                    .homeWins(home[0]).homeDraws(home[1]).homeLosses(home[2])
                    .awayWins(away[0]).awayDraws(away[1]).awayLosses(away[2])
                    .build();
        } catch (Exception e) {
            log.warn("[Standings] 행 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    // [wins, draws, losses] 반환
    // "7승0무3패" 또는 "21-0-10" (승-무-패) 형식 처리
    private int[] parseRecord(String text) {
        if (text == null || text.isBlank() || "-".equals(text.trim())) return new int[]{0, 0, 0};
        text = text.trim();
        if (text.contains("승") || text.contains("패")) {
            return new int[]{extractInt(text, "승"), extractInt(text, "무"), extractInt(text, "패")};
        }
        String[] parts = text.split("-");
        if (parts.length >= 3) {
            return new int[]{parseIntCell(parts[0]), parseIntCell(parts[1]), parseIntCell(parts[2])};
        }
        return new int[]{0, 0, 0};
    }

    // "3연승"→"W3", "2연패"→"L2"
    private String parseStreak(String text) {
        if (text == null || text.isBlank() || "-".equals(text.trim())) return null;
        text = text.trim();
        if (text.contains("승")) return "W" + extractInt(text, "승");
        if (text.contains("패")) return "L" + extractInt(text, "패");
        return text;
    }

    // "3연승"에서 delimiter(승) 앞의 숫자 추출
    private int extractInt(String text, String delimiter) {
        int idx = text.indexOf(delimiter);
        if (idx <= 0) return 0;
        String num = text.substring(0, idx).replaceAll("[^0-9]", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }

    private BigDecimal parsePct(String text) {
        if (text == null || text.isBlank() || "-".equals(text.trim())) return BigDecimal.ZERO;
        try { return new BigDecimal(text.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private BigDecimal parseGamesBehind(String text) {
        if (text == null || text.isBlank() || "-".equals(text.trim()) || "0".equals(text.trim()))
            return BigDecimal.ZERO;
        try { return new BigDecimal(text.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private int parseIntCell(String text) {
        if (text == null || text.isBlank()) return 0;
        try { return Integer.parseInt(text.trim().replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 0; }
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
