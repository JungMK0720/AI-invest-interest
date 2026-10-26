package com.finmate.practice;

import com.finmate.common.ApiException;
import java.util.List;
import org.springframework.stereotype.Component;
import static com.finmate.practice.PracticeDtos.*;

@Component
public class PracticeCatalog {
    public static final String VERSION = "portfolio-shock-v1";
    private final List<ProductSpec> products = List.of(
        p("cash", "현금 보유", "현금", "CASH", "없음", 0, 0, 1, false, "cash", "이자가 붙지 않는 대기 자금입니다. 예금 이자를 대신 계산하지 않습니다."),
        p("stock", "개별 주식 예시", "주식", "SPOT", "주가", 0, 0, 1, false, "stocks", "입력한 주가 변동률을 그대로 적용합니다. 기업별 부도·배당은 제외합니다."),
        p("equity-etf", "주식형 ETF 예시", "주식", "SPOT", "주가", 0, .3, 1, false, "etf", "같은 주가 충격을 적용하고 연 보수를 차감합니다. ETF 보유만으로 분산효과를 가정하지 않습니다."),
        p("equity-fund", "주식형 펀드 예시", "주식", "SPOT", "주가", 0, 1.2, 1, false, "funds", "주가 변동과 보수의 영향을 나눠 봅니다. 보수 값은 설명용 가정입니다."),
        p("global-equity", "환노출 해외 주식 ETF 예시", "주식", "SPOT", "주가", 0, .4, 1, true, "etf", "현지 주가와 환율 변화를 곱해 원화 수익을 계산합니다. 환율 상승은 외화 가치 상승입니다."),
        p("short-bond", "단기 채권 예시", "채권", "BOND", "금리", 2, 0, 1, false, "bonds", "수정 듀레이션 2를 기본 가정으로 금리 변화의 가격 영향을 근사합니다."),
        p("long-bond", "장기 채권 예시", "채권", "BOND", "금리", 8, 0, 1, false, "bonds", "같은 금리 변화에서 단기 채권보다 큰 가격 변화를 관찰합니다. 이자 수령액은 제외합니다."),
        p("corporate-bond", "회사채 예시", "채권", "CREDIT_BOND", "금리·신용 스프레드", 5, .2, 1, false, "bonds", "기준금리와 신용 스프레드 변화를 함께 수익률 변화로 적용합니다."),
        p("bond-fund", "채권형 펀드 예시", "채권", "CREDIT_BOND", "금리·신용 스프레드", 4, .5, 1, false, "funds", "편입 채권 전체의 대표 듀레이션을 가정합니다. 개별채권 만기상환 구조와 다릅니다."),
        p("gold", "원화 기준 금 현물 예시", "금", "SPOT", "금 가격", 0, .2, 1, false, "gold", "원화로 정한 금 가격 충격입니다. 보관비용은 연 비용 입력으로 단순화합니다."),
        p("gold-foreign", "환노출 금 ETF 예시", "금", "SPOT", "금 가격", 0, .5, 1, true, "gold", "외화 기준 금 가격과 환율 효과를 분리합니다. 실제 상품 추적오차는 제외합니다."),
        p("reit", "리츠 예시", "부동산", "SPOT", "부동산 관련 가격", 0, .6, 1, false, "real-estate", "입력 충격은 리츠 시장가격 변화의 가정입니다. 건물 가격과 리츠 주가가 같다는 뜻은 아닙니다."),
        p("property", "직접 부동산 예시", "부동산", "SPOT", "부동산 관련 가격", 0, .8, 1, false, "real-estate", "평가가격 변화만 계산합니다. 임대수익·거래세·매도 소요시간은 계산하지 않습니다."),
        p("margin-stock", "신용 주식 예시", "주식", "MARGIN", "주가", 0, .1, 2, false, "margin", "투입한 자기자본에 배수를 곱한 주식을 보유하고, 차입금의 이자를 별도 차감합니다."),
        p("futures", "주가지수 선물 매수 예시", "주식", "FUTURES", "주가", 0, .1, 5, false, "futures", "투입액은 증거금입니다. 명목금액=증거금×배수이며 추가 손실이 가능해 잔액을 0에서 자르지 않습니다."),
        p("call", "콜옵션 매수 예시", "옵션", "CALL", "만기 주가", 0, 0, 1, false, "options", "기초자산 시작값 100, 입력한 행사가·프리미엄으로 만기 손익을 계산합니다. 매도 포지션이 아닙니다."),
        p("put", "풋옵션 매수 예시", "옵션", "PUT", "만기 주가", 0, 0, 1, false, "options", "기초자산 하락 시 내재가치가 발생합니다. 만기 이전 시가와 시간가치 계산은 제외합니다.")
    );
    public List<ProductSpec> products() { return products; }
    public ProductSpec product(String id) { return products.stream().filter(p -> p.id().equals(id)).findFirst()
            .orElseThrow(() -> ApiException.badRequest("지원하지 않는 연습 상품입니다: " + id)); }
    public List<Scenario> scenarios() { return List.of(
        new Scenario("quiet", "가격 변화 없음", "가격을 고정하고 기간에 따른 비용만 확인합니다.", s(0,0,0,0,0,0,30)),
        new Scenario("equity-down", "주가만 20% 하락", "다른 요인을 고정합니다. 예측이나 실제 과거 사건이 아닙니다.", s(-20,0,0,0,0,0,30)),
        new Scenario("rate-up", "금리만 1%p 상승", "금리 충격에 대한 채권 듀레이션의 차이를 확인합니다.", s(0,1,0,0,0,0,30)),
        new Scenario("fx-up", "환율만 10% 상승", "외화 가치가 원화 대비 10% 상승한 경우입니다.", s(0,0,0,0,0,10,30)),
        new Scenario("stress", "복합 하락 가정", "주가 -25%, 금리 +1%p, 스프레드 +1%p, 금 +5%, 부동산 -15%, 환율 +8%를 사용자가 정한 가정으로 적용합니다.", s(-25,1,1,5,-15,8,30)),
        new Scenario("equity-up", "주가만 20% 상승", "같은 배분에서도 상승 시 노출 차이가 어떻게 작용하는지 확인합니다.", s(20,0,0,0,0,0,30))
    ); }
    private static Shocks s(double e,double r,double c,double g,double p,double f,int d) { return new Shocks(e,r,c,g,p,f,d); }
    private static ProductSpec p(String id,String n,String a,String m,String driver,double d,double fee,double l,boolean f,String c,String x) {
        return new ProductSpec(id,n,a,m,driver,d,fee,l,f,c,x);
    }
    public Methodology methodology() { return new Methodology(VERSION, "실제 시세 없이 사용자가 정한 충격으로 계산하는 교육 실험", List.of(
        "금액과 기본 보수·듀레이션은 가상 예시이며 실제 판매 상품의 조건이 아닙니다.",
        "주가·금 가격·부동산 가격·환율은 독립 입력입니다. 상관관계나 발생 확률을 추정하지 않습니다.",
        "채권 가격은 -수정 듀레이션×수익률 변화로 근사합니다. 볼록성·이자·부도는 제외합니다.",
        "옵션은 기초자산 시작값 100의 매수 포지션 만기 손익입니다. 입력 기간은 비용에만 사용됩니다.",
        "선물·신용 주식은 마지막 시점 손익입니다. 중간 마진콜·강제청산·슬리피지는 재현하지 않습니다.",
        "연 비용·차입 이자는 초기 금액에 기간/365로 단순 적용합니다. 매매세금·배당·임대료는 제외합니다.",
        "레버리지 ETF의 매일 재조정은 기존 개별 개념 실험기에서 다룹니다. 이 실험의 신용 주식과 다릅니다."), List.of(
        new Source("FINRA: 금리 변화와 듀레이션", "https://www.finra.org/investors/insights/bonds-interest-rate-changes-duration"),
        new Source("Investor.gov: 마진 계좌", "https://www.investor.gov/introduction-investing/general-resources/news-alerts/alerts-bulletins/investor-bulletins-29"),
        new Source("OIC: 콜옵션 매수", "https://www.optionseducation.org/strategies/all-strategies/long-call"),
        new Source("Investor.gov: 레버리지·인버스 ETF", "https://www.investor.gov/introduction-investing/general-resources/news-alerts/alerts-bulletins/investor-alerts/sec")));
    }
}
