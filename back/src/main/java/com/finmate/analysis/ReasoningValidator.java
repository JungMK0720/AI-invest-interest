package com.finmate.analysis;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 보수적인 1차 출력 검사. 의미 검증이나 모든 상품명 검출을 보장하지 않는다. */
@Component
public class ReasoningValidator {
    private static final Pattern DISALLOWED = Pattern.compile(
            "보장|확실한?\\s*수익|무위험|매수|매도|구매|투자하|종목|추천|권유|수익률|수익.{0,12}(예상|예측)|[0-9]+\\s*[%％]|https?://",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCORE = Pattern.compile("(\\d+)\\s*점");

    public boolean accepts(String text, ReasoningRequest request) {
        if (text == null || text.isBlank() || text.codePointCount(0, text.length()) > 200
                || DISALLOWED.matcher(text).find()) return false;
        for (InvestorProfileType profile : InvestorProfileType.values()) {
            if (profile != request.profileType() && text.contains(profile.label())) return false;
        }
        var scores = SCORE.matcher(text);
        while (scores.find()) {
            if (!scores.group(1).equals(Integer.toString(request.totalScore()))) return false;
        }
        return text.contains(request.profileType().label());
    }
}
