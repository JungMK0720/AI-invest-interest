package com.finmate.practice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PracticeEngineTest {
    private PracticeEngine engine;
    private PracticeCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new PracticeCatalog();
        engine = new PracticeEngine(catalog);
    }

    @Test
    void sameShockShowsBondDurationDifference() {
        var before = portfolio(10_000_000d, holding("short-bond", 5_000_000d), holding("long-bond", 5_000_000d));
        var result = engine.analyze(new PracticeDtos.AnalyzeRequest(before, shocks(0, 1, 0, 0, 0, 0, 30)));

        var shortPosition = result.positions().stream().filter(p -> p.productId().equals("short-bond")).findFirst().orElseThrow();
        var longPosition = result.positions().stream().filter(p -> p.productId().equals("long-bond")).findFirst().orElseThrow();
        assertThat(longPosition.profitLoss()).isLessThan(shortPosition.profitLoss());
        assertThat(result.lessons()).anyMatch(lesson -> lesson.contains("금리"));
    }

    @Test
    void compareExplainsAllocationChange() {
        var before = portfolio(10_000_000d, holding("stock", 8_000_000d), holding("cash", 2_000_000d));
        var after = portfolio(10_000_000d, holding("stock", 2_000_000d), holding("cash", 8_000_000d));
        var result = engine.compare(new PracticeDtos.CompareRequest(before, after, shocks(-20, 0, 0, 0, 0, 0, 30)));

        assertThat(result.profitLossDifference()).isGreaterThan(0);
        assertThat(result.interpretation()).contains("변경 후");
    }

    @Test
    void optionUsesStrikeAndPremiumRatherThanStockReturnDirectly() {
        var portfolio = portfolio(10_000_000d, holding("call", 1_000_000d));
        var result = engine.analyze(new PracticeDtos.AnalyzeRequest(portfolio, shocks(20, 0, 0, 0, 0, 0, 30)));
        var position = result.positions().getFirst();

        assertThat(position.effects()).anyMatch(effect -> effect.label().equals("가격 충격"));
        assertThat(position.observations()).anyMatch(observation -> observation.contains("옵션 프리미엄"));
    }

    private PracticeDtos.Portfolio portfolio(double budget, PracticeDtos.Holding... holdings) {
        return new PracticeDtos.Portfolio(budget, List.of(holdings));
    }

    private PracticeDtos.Holding holding(String id, double amount) {
        var product = catalog.product(id);
        return new PracticeDtos.Holding(id + "-row", id, product.name(), amount,
                product.annualFeePercent(), product.duration() > 0 ? product.duration() : 5,
                product.leverage(), 105d, 2.5d, 5d);
    }

    private PracticeDtos.Shocks shocks(double equity, double rate, double spread, double gold,
                                       double realEstate, double fx, int days) {
        return new PracticeDtos.Shocks(equity, rate, spread, gold, realEstate, fx, days);
    }
}
