package com.finmate.learning;

/** 결과 표시용 포맷 헬퍼. 금액은 원 단위 천 단위 구분, 비율은 소수 둘째 자리까지. */
final class SimulationFormat {

    private SimulationFormat() {
    }

    static String money(double value) {
        return String.format("%,.0f원", value);
    }

    /** 부호를 항상 붙인다. 손익 표시에 쓴다. */
    static String signedMoney(double value) {
        return (value >= 0 ? "+" : "-") + String.format("%,.0f원", Math.abs(value));
    }

    static String percent(double ratio) {
        return String.format("%.2f%%", ratio * 100);
    }

    static String signedPercent(double ratio) {
        return (ratio >= 0 ? "+" : "") + String.format("%.2f%%", ratio * 100);
    }

    static String years(double value) {
        return String.format("%.2f년", value);
    }

    static String number(double value, int decimals) {
        return String.format("%,." + decimals + "f", value);
    }

    /**
     * 앞 단어의 받침에 따라 조사를 고른다. 금액·비율을 문장에 끼워 넣을 때 "5,000만원를"처럼
     * 어색해지는 것을 막는다. 한글 음절이 아니면(숫자·기호) 받침 없음으로 본다.
     *
     * @param withJong    받침이 있을 때 쓸 조사 (을, 이, 으로)
     * @param withoutJong 받침이 없을 때 쓸 조사 (를, 가, 로)
     */
    static String josa(String word, String withJong, String withoutJong) {
        return hasFinalConsonant(word) ? withJong : withoutJong;
    }

    /** 'ㄹ' 받침은 '로/으로' 선택에서 받침 없음과 같이 취급한다. */
    static String josaRo(String word) {
        int jong = finalConsonant(word);
        return jong == 0 || jong == 8 ? "로" : "으로";
    }

    private static boolean hasFinalConsonant(String word) {
        return finalConsonant(word) != 0;
    }

    private static int finalConsonant(String word) {
        if (word == null || word.isEmpty()) {
            return 0;
        }
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) {
            return 0;
        }
        return (last - 0xAC00) % 28;
    }

    /** 1억 이상은 "1억 2,340만원"처럼 읽기 쉬운 단위로 바꾼다. 헤드라인 문장에만 쓴다. */
    static String readable(double value) {
        double abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs >= 100_000_000) {
            long eok = (long) (abs / 100_000_000);
            long man = Math.round((abs % 100_000_000) / 10_000.0);
            return man == 0 ? sign + eok + "억원" : sign + eok + "억 " + String.format("%,d", man) + "만원";
        }
        if (abs >= 10_000) {
            return sign + String.format("%,d", Math.round(abs / 10_000.0)) + "만원";
        }
        return money(value);
    }
}
