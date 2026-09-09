package com.finmate.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenDART 경계의 최소 구현. 운영에서는 응답 JSON을 원문 저장소에 먼저 보관한 뒤 정규화한다.
 * API 키가 없을 때 애플리케이션 기동을 막지 않으며, 호출은 수집 작업에서만 수행한다.
 */
@Component
public class OpenDartProvider implements MarketDataProvider {
    private final String apiKey;
    private final HttpClient client = HttpClient.newHttpClient();
    public OpenDartProvider(@Value("${finmate.integrations.opendart.api-key:}") String apiKey) { this.apiKey = apiKey; }
    @Override public String providerKey() { return "opendart"; }
    public boolean isConfigured() { return !apiKey.isBlank(); }
    /** 공시 목록 수집의 실제 endpoint 호출 지점. rate limit과 content hash는 상위 수집기가 관리한다. */
    public String fetchDisclosureList(String corpCode, String start, String end) {
        if (!isConfigured()) throw new IllegalStateException("OPENDART_API_KEY가 설정되지 않았습니다.");
        try {
            URI uri = URI.create("https://opendart.fss.or.kr/api/list.json?crtfc_key=" + apiKey
                    + "&corp_code=" + corpCode + "&bgn_de=" + start + "&end_de=" + end + "&page_count=100");
            return client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) { throw new IllegalStateException("OpenDART 수집에 실패했습니다.", e); }
    }
    @Override public List<RawDataRecord> fetch(FetchRequest request) { return List.of(); }
}
