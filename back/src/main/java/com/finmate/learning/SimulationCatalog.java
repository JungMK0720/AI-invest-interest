package com.finmate.learning;

import com.finmate.learning.SimulationDtos.Choice;
import com.finmate.learning.SimulationDtos.InputField;
import com.finmate.learning.SimulationDtos.ScenarioSpec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 모의투자 시나리오 정의.
 *
 * <p>시나리오는 "상품을 사고파는 흉내"가 아니라 "개념 하나가 돈에 어떻게 작용하는지"를 실험하는 단위다.
 * 그래서 시나리오마다 질문(question)을 먼저 두고, 사용자가 입력을 바꿔 그 질문의 답이 어떻게 달라지는지
 * 보게 한다. 계산식은 {@link SimulationEngine}에 있고 여기에는 화면이 필요로 하는 명세만 둔다.
 */
@Service
public class SimulationCatalog {

    private final Map<String, ScenarioSpec> byId = new LinkedHashMap<>();

    public SimulationCatalog() {
        register(pricePath());
        register(dcaVsLumpSum());
        register(bondRateShock());
        register(feeDrag());
        register(etfTracking());
        register(diversification());
        register(liquidityExit());
        register(leverageDecay());
        register(optionPayoff());
        register(futuresMargin());
        register(depositInflation());
    }

    private void register(ScenarioSpec spec) {
        byId.put(spec.id(), spec);
    }

    public List<ScenarioSpec> list() {
        return List.copyOf(byId.values());
    }

    public ScenarioSpec find(String id) {
        ScenarioSpec spec = byId.get(id);
        if (spec == null) {
            throw new IllegalArgumentException("모의투자 시나리오를 찾을 수 없습니다: " + id);
        }
        return spec;
    }

    public boolean contains(String id) {
        return byId.containsKey(id);
    }

    /** 개념 하나에 연결된 시나리오들. 개념 화면의 모의투자 탭이 이 목록을 쓴다. */
    public List<ScenarioSpec> forConcept(String slug) {
        return byId.values().stream().filter(spec -> spec.conceptSlugs().contains(slug)).toList();
    }

    private static ScenarioSpec pricePath() {
        return new ScenarioSpec(
                "price-path",
                "가격이 오르내리는 동안 내 돈",
                "기대수익률이 같아도 결과가 같을까?",
                "같은 기대수익률과 변동성에서도 실제 경로에 따라 결과가 달라지는 것을 확인합니다.",
                "월 수익률을 로그정규 분포로 생성하고 기하 누적합니다. 기대수익률은 평균일 뿐 실현값이 아닙니다.",
                List.of("risk-return", "volatility", "drawdown", "stocks", "etf"),
                List.of(
                        InputField.number("amount", "투자금액", "원", 100_000, 100_000_000, 100_000, 3_000_000, "한 번에 넣는 금액입니다."),
                        InputField.number("months", "투자 기간", "개월", 6, 240, 6, 36, "길게 볼수록 경로의 폭이 넓어집니다."),
                        InputField.number("annualReturn", "가정 연 기대수익률", "%", -10, 20, 0.5, 7, "예측이 아니라 실험용 가정값입니다."),
                        InputField.number("annualVolatility", "가정 연 변동성", "%", 1, 60, 1, 18, "값이 클수록 경로가 크게 흔들립니다."),
                        InputField.choice("style", "매수 방식", List.of(
                                new Choice("LUMP_SUM", "한 번에 매수"),
                                new Choice("DCA", "매월 나눠 매수")), "LUMP_SUM", "같은 금액을 언제 넣는지에 따라 경험이 달라집니다.")),
                true,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec dcaVsLumpSum() {
        return new ScenarioSpec(
                "dca-vs-lumpsum",
                "한 번에 vs 나눠서",
                "같은 돈을 언제 넣느냐가 결과를 바꿀까?",
                "동일한 시장 경로에서 거치식과 적립식을 같은 조건으로 비교합니다.",
                "같은 가격 경로에 두 매수 방식을 적용해 평균 매입단가와 최종 평가액을 비교합니다.",
                List.of("time-horizon", "compounding", "goal-based", "volatility"),
                List.of(
                        InputField.number("amount", "총 투자금액", "원", 600_000, 100_000_000, 100_000, 6_000_000, "두 방식에 같은 총액을 씁니다."),
                        InputField.number("months", "투자 기간", "개월", 6, 120, 6, 24, "적립식은 이 기간에 걸쳐 나눠 넣습니다."),
                        InputField.number("annualReturn", "가정 연 기대수익률", "%", -10, 20, 0.5, 6, "실험용 가정값입니다."),
                        InputField.number("annualVolatility", "가정 연 변동성", "%", 1, 60, 1, 20, "변동성이 클수록 두 방식의 차이가 커집니다."),
                        InputField.choice("shape", "시장 흐름 가정", List.of(
                                new Choice("RANDOM", "무작위 변동"),
                                new Choice("RISING", "꾸준히 상승"),
                                new Choice("DIP_RECOVER", "먼저 하락 후 회복"),
                                new Choice("FALLING", "꾸준히 하락")), "DIP_RECOVER", "흐름을 바꾸면 유리한 방식이 바뀝니다.")),
                true,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec bondRateShock() {
        return new ScenarioSpec(
                "bond-rate-shock",
                "금리가 움직일 때 채권 가격",
                "금리가 1%p 오르면 내 채권은 얼마나 떨어질까?",
                "현금흐름 할인으로 채권 가격을 직접 계산하고 만기 보유와 중도 매도를 비교합니다.",
                "가격 = Σ 이자/(1+y)^t + 액면/(1+y)^n 으로 계산하고, 듀레이션과 볼록성으로 민감도를 설명합니다.",
                List.of("bonds", "bond-price-yield", "duration", "credit-risk"),
                List.of(
                        InputField.number("faceAmount", "액면금액", "원", 1_000_000, 100_000_000, 1_000_000, 10_000_000, "만기에 돌려받기로 한 원금입니다."),
                        InputField.number("couponRate", "표면금리", "%", 0, 12, 0.25, 3, "매년 받는 이자의 비율입니다."),
                        InputField.number("maturityYears", "잔존 만기", "년", 1, 30, 1, 10, "만기가 길수록 금리에 민감합니다."),
                        InputField.number("marketYield", "현재 시장금리", "%", 0.5, 12, 0.25, 3, "지금 같은 채권에 요구되는 수익률입니다."),
                        InputField.number("yieldChangeBp", "금리 변동", "bp", -300, 300, 25, 100, "100bp = 1%p 입니다."),
                        InputField.choice("plan", "보유 계획", List.of(
                                new Choice("HOLD", "만기까지 보유"),
                                new Choice("SELL", "금리 변동 직후 매도")), "SELL", "언제 파는지가 손익을 바꿉니다.")),
                false,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec feeDrag() {
        return new ScenarioSpec(
                "fee-drag",
                "보수 0.1%의 20년",
                "작아 보이는 보수가 오래가면 얼마가 될까?",
                "총보수만 다른 두 상품을 같은 수익률로 굴려 누적 차이를 봅니다.",
                "매년 (1+수익률)×(1-보수)로 복리 계산해 두 경로의 차이를 누적합니다.",
                List.of("cost", "expense-ratio", "funds", "etf", "compounding", "pension"),
                List.of(
                        InputField.number("amount", "투자금액", "원", 1_000_000, 500_000_000, 1_000_000, 30_000_000, "처음 넣는 금액입니다."),
                        InputField.number("years", "투자 기간", "년", 1, 40, 1, 20, "기간이 길수록 차이가 커집니다."),
                        InputField.number("annualReturn", "가정 연 수익률", "%", -5, 15, 0.5, 6, "두 상품에 같은 수익률을 가정합니다."),
                        InputField.number("feeA", "상품 A 총보수", "%", 0, 3, 0.01, 0.07, "저보수 인덱스형의 예시 수준입니다."),
                        InputField.number("feeB", "상품 B 총보수", "%", 0, 3, 0.01, 1.2, "운용보수가 높은 상품의 예시입니다.")),
                false,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec etfTracking() {
        return new ScenarioSpec(
                "etf-tracking",
                "지수는 올랐는데 내 수익은 왜 다를까",
                "지수 수익률과 내 계좌 수익률의 차이는 어디서 생길까?",
                "보수·추적오차·괴리율·스프레드를 단계별로 차감해 실제 손에 남는 수익을 확인합니다.",
                "지수 수익에서 총보수, 추적오차, 매수 시 괴리, 매도 시 괴리, 매매 스프레드를 순서대로 차감합니다.",
                List.of("etf", "etf-premium-discount", "tracking-error", "nav", "trading-cost"),
                List.of(
                        InputField.number("amount", "투자금액", "원", 1_000_000, 200_000_000, 1_000_000, 10_000_000, "ETF 매수 금액입니다."),
                        InputField.number("years", "보유 기간", "년", 1, 20, 1, 5, "보수는 보유 기간에 비례해 누적됩니다."),
                        InputField.number("indexReturn", "지수 연 수익률", "%", -10, 20, 0.5, 8, "기초지수의 가정 수익률입니다."),
                        InputField.number("ter", "총보수(TER)", "%", 0, 2, 0.01, 0.25, "매년 순자산에서 차감됩니다."),
                        InputField.number("trackingErrorBp", "연 추적오차", "bp", -200, 200, 5, 15, "지수와 NAV가 벌어지는 정도입니다."),
                        InputField.number("premiumBuyBp", "매수 시 괴리율", "bp", -300, 300, 5, 30, "양수면 NAV보다 비싸게 산 것입니다."),
                        InputField.number("premiumSellBp", "매도 시 괴리율", "bp", -300, 300, 5, -20, "음수면 NAV보다 싸게 판 것입니다."),
                        InputField.number("spreadBp", "호가 스프레드", "bp", 0, 200, 5, 15, "매수·매도 각각 절반씩 부담한다고 봅니다.")),
                false,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec diversification() {
        return new ScenarioSpec(
                "diversification",
                "둘로 나누면 정말 덜 흔들릴까",
                "상관관계가 얼마일 때 분산이 의미가 있을까?",
                "두 자산의 비중과 상관계수를 바꿔 포트폴리오 변동성과 최대낙폭을 확인합니다.",
                "σp = √(wA²σA² + wB²σB² + 2wAwBρσAσB) 를 계산하고, 촐레스키 분해로 상관된 경로를 생성합니다.",
                List.of("diversification", "correlation", "asset-allocation", "rebalancing"),
                List.of(
                        InputField.number("amount", "투자금액", "원", 1_000_000, 200_000_000, 1_000_000, 10_000_000, "두 자산에 나눠 담습니다."),
                        InputField.number("months", "투자 기간", "개월", 12, 240, 6, 60, "경로 길이입니다."),
                        InputField.number("weightA", "자산 A 비중", "%", 0, 100, 5, 60, "나머지는 자산 B에 담깁니다."),
                        InputField.number("returnA", "A 연 기대수익률", "%", -5, 20, 0.5, 8, "예: 주식형 가정."),
                        InputField.number("volA", "A 연 변동성", "%", 1, 60, 1, 20, "값이 클수록 크게 흔들립니다."),
                        InputField.number("returnB", "B 연 기대수익률", "%", -5, 20, 0.5, 3, "예: 채권형 가정."),
                        InputField.number("volB", "B 연 변동성", "%", 1, 60, 1, 6, "값이 작을수록 안정적입니다."),
                        InputField.number("correlation", "상관계수", "", -1, 1, 0.1, 0.1, "1이면 함께 움직여 분산 효과가 사라집니다.")),
                true,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec liquidityExit() {
        return new ScenarioSpec(
                "liquidity-exit",
                "필요할 때 팔 수 있을까",
                "급하게 팔면 얼마를 손해 볼까?",
                "보유금액과 종목의 거래대금, 매도 속도에 따라 현금화 비용이 어떻게 달라지는지 봅니다.",
                "체결비용 = 스프레드 절반 + 시장충격. 시장충격은 참여율의 제곱근에 비례하는 근사식을 씁니다.",
                List.of("liquidity", "bid-ask-spread", "market-impact", "redemption-period", "reits"),
                List.of(
                        InputField.number("holdingAmount", "현금화할 금액", "원", 1_000_000, 500_000_000, 1_000_000, 50_000_000, "지금 팔아야 하는 금액입니다."),
                        InputField.number("avgDailyValue", "종목 일평균 거래대금", "원", 10_000_000, 500_000_000_000L, 10_000_000, 2_000_000_000L, "거래가 적을수록 팔기 어렵습니다."),
                        InputField.number("spreadBp", "호가 스프레드", "bp", 1, 500, 1, 20, "사자·팔자 호가의 차이입니다."),
                        InputField.choice("urgency", "매도 속도", List.of(
                                new Choice("IMMEDIATE", "오늘 전량 매도"),
                                new Choice("THREE_DAYS", "3일에 나눠 매도"),
                                new Choice("TEN_DAYS", "10일에 나눠 매도")), "IMMEDIATE", "급할수록 비용이 커집니다.")),
                false,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec leverageDecay() {
        return new ScenarioSpec(
                "leverage-decay",
                "2배 상품은 정말 2배일까",
                "지수가 제자리로 돌아오면 레버리지도 제자리일까?",
                "일별 재조정 레버리지의 누적 결과를 지수 자체와 비교합니다.",
                "레버리지 상품은 매일 배수를 맞추므로 일간 수익률이 곱해집니다. 등락이 반복되면 지수가 제자리여도 손실이 남습니다.",
                List.of("leverage", "leveraged-etf", "liquidation", "derivatives", "volatility"),
                List.of(
                        InputField.number("amount", "투자금액", "원", 1_000_000, 100_000_000, 1_000_000, 5_000_000, "레버리지 상품에 넣는 금액입니다."),
                        InputField.number("days", "보유 기간", "거래일", 20, 500, 10, 120, "기간이 길수록 변동성 손실이 쌓입니다."),
                        InputField.number("multiple", "레버리지 배수", "배", 1, 5, 1, 2, "1이면 지수와 같습니다."),
                        InputField.number("annualReturn", "지수 연 기대수익률", "%", -20, 20, 1, 0, "0으로 두면 순수한 변동성 효과만 봅니다."),
                        InputField.number("annualVolatility", "지수 연 변동성", "%", 5, 80, 1, 25, "높을수록 변동성 손실이 커집니다.")),
                true,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec optionPayoff() {
        return new ScenarioSpec(
                "option-payoff",
                "옵션 손익은 어디서 갈릴까",
                "옵션을 사는 쪽과 파는 쪽의 손실 구조는 어떻게 다를까?",
                "만기 시 기초자산 가격에 따른 손익 곡선과 손익분기점을 계산합니다.",
                "만기 손익 = (내재가치 - 프리미엄) × 수량. 매도자는 부호가 반대이며 이익이 프리미엄으로 제한됩니다.",
                List.of("options", "option-payoff", "time-value", "derivatives"),
                List.of(
                        InputField.choice("optionType", "옵션 종류", List.of(
                                new Choice("CALL", "콜(살 권리)"),
                                new Choice("PUT", "풋(팔 권리)")), "CALL", "권리의 방향입니다."),
                        InputField.choice("side", "포지션", List.of(
                                new Choice("BUY", "매수(권리를 산다)"),
                                new Choice("SELL", "매도(권리를 판다)")), "BUY", "매도는 의무를 지는 쪽입니다."),
                        InputField.number("spot", "현재 기초자산 가격", "원", 1_000, 1_000_000, 1_000, 100_000, "지금 가격입니다."),
                        InputField.number("strike", "행사가격", "원", 1_000, 1_000_000, 1_000, 105_000, "권리를 행사할 가격입니다."),
                        InputField.number("premium", "프리미엄", "원", 10, 100_000, 10, 2_500, "권리의 값입니다."),
                        InputField.number("quantity", "수량", "계약", 1, 100, 1, 10, "계약 수입니다.")),
                false,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec futuresMargin() {
        return new ScenarioSpec(
                "futures-margin",
                "증거금은 언제 부족해질까",
                "가격이 얼마나 움직이면 추가 납입 요구를 받을까?",
                "일일정산으로 평가손익을 반영하며 유지증거금 아래로 내려가는 시점을 찾습니다.",
                "매일 정산손익을 예탁금에 반영하고, 예탁금이 유지증거금 미만이면 추가 납입(마진콜)이 발생합니다.",
                List.of("futures", "margin", "roll-cost", "derivatives", "leverage"),
                List.of(
                        InputField.number("deposit", "예탁금", "원", 1_000_000, 500_000_000, 1_000_000, 20_000_000, "계좌에 넣은 돈입니다."),
                        InputField.number("indexLevel", "진입 지수", "p", 100, 10_000, 10, 350, "계약 진입 시점의 지수입니다."),
                        InputField.number("multiplier", "거래승수", "원/p", 1_000, 500_000, 1_000, 250_000, "지수 1p당 손익 금액입니다."),
                        InputField.number("contracts", "계약 수", "계약", 1, 20, 1, 1, "많을수록 위험이 커집니다."),
                        InputField.number("initialMarginRate", "개시증거금률", "%", 3, 30, 0.5, 10.5, "진입에 필요한 비율입니다."),
                        InputField.number("maintenanceMarginRate", "유지증거금률", "%", 2, 25, 0.5, 7, "이 아래로 내려가면 추가 납입입니다."),
                        InputField.number("days", "관찰 기간", "거래일", 5, 120, 5, 20, "일일정산을 반복할 기간입니다."),
                        InputField.number("annualVolatility", "지수 연 변동성", "%", 5, 60, 1, 20, "변동성이 클수록 마진콜이 빨라집니다.")),
                true,
                SimulationDtos.SIMULATION_NOTICE);
    }

    private static ScenarioSpec depositInflation() {
        return new ScenarioSpec(
                "deposit-inflation",
                "원금은 지켰는데 살 수 있는 건 줄었다",
                "예금 이자는 물가를 이겼을까?",
                "세후 이자와 물가상승률을 함께 계산해 구매력 기준 결과를 봅니다.",
                "세후 이자율로 복리 계산한 금액을 (1+물가상승률)^년 으로 나눠 실질 구매력을 구합니다.",
                List.of("cash", "deposit-protection", "mmf", "risk-return", "goal-based"),
                List.of(
                        InputField.number("amount", "예치금액", "원", 1_000_000, 500_000_000, 1_000_000, 30_000_000, "예금에 넣는 금액입니다."),
                        InputField.number("years", "예치 기간", "년", 1, 30, 1, 10, "복리로 계산합니다."),
                        InputField.number("depositRate", "연 예금금리", "%", 0.1, 10, 0.1, 3.2, "세전 금리입니다."),
                        InputField.number("taxRate", "이자소득세율", "%", 0, 30, 0.1, 15.4, "일반과세 기준 예시입니다."),
                        InputField.number("inflationRate", "연 물가상승률", "%", 0, 10, 0.1, 2.5, "구매력을 깎는 속도입니다.")),
                false,
                SimulationDtos.SIMULATION_NOTICE);
    }
}
