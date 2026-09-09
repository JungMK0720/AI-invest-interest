package com.finmate.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 모의투자 엔진 테스트.
 *
 * <p>확인하는 것은 "숫자가 그럴듯한가"가 아니라 금융 구조가 실제로 재현되는가다.
 * 금리가 오르면 채권 가격이 내려야 하고, 보수가 높으면 결과가 낮아야 하며,
 * 같은 seed면 같은 결과가 나와야 한다.
 */
class SimulationEngineTest {

    private final SimulationCatalog catalog = new SimulationCatalog();
    private final SimulationEngine engine = new SimulationEngine(catalog);

    @Test
    void 모든_시나리오가_기본값으로_실행된다() {
        for (SimulationDtos.ScenarioSpec spec : catalog.list()) {
            SimulationDtos.RunResponse response = engine.run(spec.id(), new SimulationDtos.RunRequest(Map.of(), null));
            assertThat(response.headline()).as(spec.id()).isNotBlank();
            assertThat(response.metrics()).as(spec.id()).isNotEmpty();
            assertThat(response.lessons()).as(spec.id()).isNotEmpty();
            assertThat(response.notice()).as(spec.id()).isEqualTo(SimulationDtos.SIMULATION_NOTICE);
            assertThat(response.echo()).as(spec.id()).hasSameSizeAs(spec.inputs());
        }
    }

    @Test
    void 같은_seed와_입력이면_결과가_항상_같다() {
        SimulationDtos.RunRequest request = new SimulationDtos.RunRequest(Map.of("months", "36"), 42L);
        SimulationDtos.RunResponse first = engine.run("price-path", request);
        SimulationDtos.RunResponse second = engine.run("price-path", request);
        assertThat(first.metrics()).isEqualTo(second.metrics());
        assertThat(first.series()).isEqualTo(second.series());
    }

    @Test
    void seed가_다르면_다른_시장경로가_나온다() {
        SimulationDtos.RunResponse a = engine.run("price-path", new SimulationDtos.RunRequest(Map.of(), 1L));
        SimulationDtos.RunResponse b = engine.run("price-path", new SimulationDtos.RunRequest(Map.of(), 2L));
        assertThat(a.metrics()).isNotEqualTo(b.metrics());
    }

    @Test
    void 금리가_오르면_채권가격이_내리고_만기가_길수록_민감하다() {
        double shortBond = priceChange(5);
        double longBond = priceChange(20);
        assertThat(shortBond).isNegative();
        assertThat(longBond).isNegative();
        assertThat(longBond).isLessThan(shortBond);
    }

    private double priceChange(int maturityYears) {
        SimulationDtos.RunResponse response = engine.run("bond-rate-shock", new SimulationDtos.RunRequest(
                Map.of("maturityYears", String.valueOf(maturityYears), "yieldChangeBp", "100"), null));
        return metric(response, "가격 변화율");
    }

    @Test
    void 보수가_높은_상품이_항상_적게_남는다() {
        SimulationDtos.RunResponse response = engine.run("fee-drag", new SimulationDtos.RunRequest(
                Map.of("amount", "10000000", "years", "20", "annualReturn", "6", "feeA", "0.1", "feeB", "1.5"), null));
        double gap = metric(response, "두 상품의 차이");
        assertThat(gap).isPositive();
    }

    @Test
    void 상관계수가_1이면_분산효과가_사라진다() {
        double withBenefit = metric(engine.run("diversification",
                new SimulationDtos.RunRequest(Map.of("correlation", "0"), 7L)), "분산으로 줄인 변동성");
        double noBenefit = metric(engine.run("diversification",
                new SimulationDtos.RunRequest(Map.of("correlation", "1"), 7L)), "분산으로 줄인 변동성");
        assertThat(withBenefit).isPositive();
        assertThat(noBenefit).isCloseTo(0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void 급하게_팔수록_현금화_비용이_커진다() {
        double immediate = metric(engine.run("liquidity-exit",
                new SimulationDtos.RunRequest(Map.of("urgency", "IMMEDIATE"), null)), "총 현금화 비용");
        double spread = metric(engine.run("liquidity-exit",
                new SimulationDtos.RunRequest(Map.of("urgency", "TEN_DAYS"), null)), "총 현금화 비용");
        assertThat(immediate).isGreaterThan(spread);
    }

    @Test
    void 변동성이_있으면_레버리지_결과가_단순배수보다_낮다() {
        SimulationDtos.RunResponse response = engine.run("leverage-decay", new SimulationDtos.RunRequest(
                Map.of("multiple", "3", "annualReturn", "0", "annualVolatility", "40", "days", "250"), 11L));
        double decay = metric(response, "차이 (변동성 손실)");
        assertThat(decay).isNegative();
    }

    @Test
    void 콜옵션_매수의_손익분기점은_행사가에_프리미엄을_더한_값이다() {
        SimulationDtos.RunResponse response = engine.run("option-payoff", new SimulationDtos.RunRequest(
                Map.of("optionType", "CALL", "side", "BUY", "strike", "105000", "premium", "2500"), null));
        assertThat(metric(response, "손익분기 기초자산 가격")).isEqualTo(107500.0);
    }

    @Test
    void 물가가_세후금리보다_높으면_실질_구매력이_줄어든다() {
        SimulationDtos.RunResponse response = engine.run("deposit-inflation", new SimulationDtos.RunRequest(
                Map.of("depositRate", "3", "taxRate", "15.4", "inflationRate", "4", "years", "10"), null));
        assertThat(metric(response, "실질 구매력 변화")).isNegative();
    }

    @Test
    void 잘못된_입력은_명세_범위로_보정되고_실패하지_않는다() {
        SimulationDtos.RunResponse response = engine.run("price-path", new SimulationDtos.RunRequest(
                Map.of("months", "99999", "amount", "not-a-number", "style", "UNKNOWN"), null));
        assertThat(response.echo().get("months")).isEqualTo("240");
        assertThat(response.echo().get("amount")).isEqualTo("3000000");
        assertThat(response.echo().get("style")).isEqualTo("LUMP_SUM");
    }

    @Test
    void 개념에_연결된_시나리오를_찾을_수_있다() {
        List<SimulationDtos.ScenarioSpec> forBonds = catalog.forConcept("duration");
        assertThat(forBonds).extracting(SimulationDtos.ScenarioSpec::id).contains("bond-rate-shock");
    }

    private static double metric(SimulationDtos.RunResponse response, String label) {
        return response.metrics().stream()
                .filter(metric -> metric.label().startsWith(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("지표를 찾을 수 없습니다: " + label))
                .rawValue();
    }
}
