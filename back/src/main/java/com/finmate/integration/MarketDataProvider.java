package com.finmate.integration;

import java.time.Instant;
import java.util.List;

/**
 * 외부 데이터 공급자 경계. 컨트롤러가 외부 API를 직접 호출하지 않도록 한다.
 * 구현체는 OpenDART/KRX/공식 상품 데이터 공급자별 rate limit, 재시도, 원문 저장을 책임진다.
 */
public interface MarketDataProvider {
    String providerKey();
    List<RawDataRecord> fetch(FetchRequest request);
    record FetchRequest(Instant from, Instant to, String cursor) {}
    record RawDataRecord(String externalId, String documentType, Instant publishedAt, String payload, String sourceUrl) {}
}
