package com.finmate.assessment;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** FinMATE 자체 정책: 한국 달력 기준 매월 1회. 금융권 공통 규정이 아니다. */
@Component
public class AssessmentPeriodPolicy {
    public static final String VERSION = "monthly-kst-v1";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final Clock clock;
    public AssessmentPeriodPolicy() { this(Clock.systemUTC()); }
    AssessmentPeriodPolicy(Clock clock) { this.clock = clock; }
    public Instant now() { return clock.instant(); }
    public String period() { return YearMonth.from(now().atZone(ZONE)).toString(); }
    public Instant nextAvailableAt() {
        return YearMonth.from(now().atZone(ZONE)).plusMonths(1).atDay(1).atStartOfDay(ZONE).toInstant();
    }
}
