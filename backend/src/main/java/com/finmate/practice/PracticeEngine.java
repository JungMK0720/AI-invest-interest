package com.finmate.practice;

import com.finmate.common.ApiException;
import java.util.*;
import static com.finmate.practice.PracticeDtos.*;
import org.springframework.stereotype.Service;

@Service
public class PracticeEngine {
    private static final String VERSION = PracticeCatalog.VERSION;
    private final PracticeCatalog catalog;
    public PracticeEngine(PracticeCatalog catalog) { this.catalog = catalog; }

    public Analysis analyze(AnalyzeRequest req) { return analyze(req.portfolio(), req.shocks()); }
    public Comparison compare(CompareRequest req) {
        Analysis before = analyze(req.before(), req.shocks()), after = analyze(req.after(), req.shocks());
        double diff = after.profitLoss() - before.profitLoss();
        String interpretation = Math.abs(diff) < 0.01
                ? "두 배분의 손익 차이가 거의 없습니다. 다른 충격 시나리오에서도 같은지 확인하세요."
                : (diff > 0 ? "변경 후 배분이 이 가정에서 " + money(diff) + "만큼 손실을 덜 보거나 이익을 더 냈습니다. 좋은 배분이라는 뜻은 아닙니다."
                : "변경 후 배분이 이 가정에서 " + money(Math.abs(diff)) + "만큼 더 불리했습니다. 특정 시나리오 하나로 배분을 결정하지 마세요.");
        return new Comparison(before, after, diff, interpretation);
    }

    public Analysis analyze(Portfolio portfolio, Shocks shocks) {
        validatePortfolio(portfolio);
        double invested = portfolio.holdings().stream().mapToDouble(Holding::amount).sum();
        if (invested > portfolio.budget() + 0.005) throw ApiException.badRequest("상품 금액 합계가 예산을 초과했습니다.");
        List<PositionResult> positions = new ArrayList<>();
        Map<String, Double> allocation = new LinkedHashMap<>();
        double ending = portfolio.budget() - invested;
        double grossExposure = 0, optionPremium = 0;
        for (Holding h : portfolio.holdings()) {
            ProductSpec p = catalog.product(h.productId());
            double pnl = pnl(p, h, shocks);
            double end = h.amount() + pnl - fee(h, p, shocks.days()) - borrowing(h, p, shocks.days());
            double fee = fee(h, p, shocks.days()), loan = borrowing(h, p, shocks.days());
            double weight = h.amount() / portfolio.budget() * 100;
            double exposure = exposure(p, h);
            grossExposure += exposure;
            if (p.mechanism().equals("CALL") || p.mechanism().equals("PUT")) optionPremium += h.amount();
            allocation.merge(p.assetClass(), h.amount(), Double::sum);
            List<Effect> effects = effects(p, h, shocks, pnl, fee, loan);
            List<String> observations = observations(p, h, pnl, end);
            positions.add(new PositionResult(h.rowId(), p.id(), h.label(), p.assetClass(), p.conceptSlug(), h.amount(), weight, exposure, end, end - h.amount(), (end / h.amount() - 1) * 100, effects, observations));
            ending += end;
        }
        double profit = ending - portfolio.budget();
        List<Allocation> alloc = allocation.entrySet().stream().map(e -> new Allocation(e.getKey(), e.getValue(), e.getValue()/portfolio.budget()*100)).toList();
        List<String> lessons = lessons(portfolio, shocks, positions, profit);
        List<String> limitations = List.of("가정 기반 교육 계산이며 실제 상품 가격·수익률·체결·세금을 재현하지 않습니다.", "같은 충격의 크기를 바꿔 결과의 민감도를 비교해야 합니다.", "이 결과만으로 투자 적합성이나 매매 결정을 판단하지 않습니다.");
        return new Analysis(VERSION, portfolio.budget(), portfolio.budget() - invested, ending, profit, profit/portfolio.budget()*100, grossExposure/portfolio.budget(), optionPremium, alloc, positions, lessons, limitations);
    }

    private void validatePortfolio(Portfolio p) {
        if (p == null || p.holdings() == null || p.holdings().isEmpty()) throw ApiException.badRequest("상품을 하나 이상 추가해야 합니다.");
        Set<String> rows = new HashSet<>();
        for (Holding h : p.holdings()) {
            if (!rows.add(h.rowId())) throw ApiException.badRequest("상품 행 ID가 중복되었습니다.");
            if (!finite(h.amount()) || !finite(h.annualFeePercent()) || !finite(h.duration()) || !finite(h.leverage()) || !finite(h.strike()) || !finite(h.premium()) || !finite(h.borrowingRatePercent())) throw ApiException.badRequest("숫자 입력이 유효하지 않습니다.");
        }
    }
    private static boolean finite(double n) { return !Double.isNaN(n) && !Double.isInfinite(n); }
    private double pnl(ProductSpec p, Holding h, Shocks s) {
        double r = switch (p.mechanism()) {
            case "CASH" -> 0;
            case "SPOT" -> spotShock(p, s) / 100;
            case "BOND" -> -h.duration() * s.ratePoints() / 100;
            case "CREDIT_BOND" -> -h.duration() * s.ratePoints() / 100 - s.creditSpreadPoints() / 100;
            case "MARGIN", "FUTURES" -> spotShock(p, s) / 100 * h.leverage();
            case "CALL", "PUT" -> optionReturn(p, h, s);
            default -> 0;
        };
        return h.amount() * r;
    }
    private double spotShock(ProductSpec p, Shocks s) {
        double r = switch (p.assetClass()) { case "주식" -> s.equityPercent()/100; case "금" -> s.goldPercent()/100; case "부동산" -> s.realEstatePercent()/100; default -> 0; };
        return p.foreignCurrency() ? ((1+r)*(1+s.fxPercent()/100)-1)*100 : r*100;
    }
    private double optionReturn(ProductSpec p, Holding h, Shocks s) {
        double finalSpot = 100 * (1 + s.equityPercent()/100);
        double intrinsic = p.mechanism().equals("CALL") ? Math.max(finalSpot-h.strike(), 0) : Math.max(h.strike()-finalSpot, 0);
        return intrinsic / h.premium() - 1;
    }
    private double exposure(ProductSpec p, Holding h) { return switch (p.mechanism()) { case "MARGIN", "FUTURES" -> h.amount()*h.leverage(); case "CALL", "PUT" -> 0; default -> h.amount(); }; }
    private double fee(Holding h, ProductSpec p, int days) { double rate = h.annualFeePercent() > 0 ? h.annualFeePercent() : p.annualFeePercent(); return h.amount()*rate/100*days/365; }
    private double borrowing(Holding h, ProductSpec p, int days) { return p.mechanism().equals("MARGIN") ? h.amount()*(h.leverage()-1)*h.borrowingRatePercent()/100*days/365 : 0; }
    private List<Effect> effects(ProductSpec p, Holding h, Shocks s, double pnl, double fee, double loan) {
        List<Effect> x = new ArrayList<>();
        if (p.mechanism().equals("BOND") || p.mechanism().equals("CREDIT_BOND")) x.add(new Effect("금리", -h.amount()*h.duration()*s.ratePoints()/100, "-듀레이션 × 금리변화", "금리가 오르면 채권 가격이 내려가는 민감도입니다."));
        if (p.mechanism().equals("CREDIT_BOND")) x.add(new Effect("신용 스프레드", -h.amount()*s.creditSpreadPoints()/100, "-스프레드 변화", "발행자 위험 보상이 커질 때 가격에 주는 충격입니다."));
        if (p.foreignCurrency()) x.add(new Effect("환율", h.amount()*(1+spotShock(p,s)/100)*s.fxPercent()/100, "외화자산 × 환율 변화", "환율이 오르면 외화자산의 원화 가치가 변합니다."));
        if (p.mechanism().equals("MARGIN") || p.mechanism().equals("FUTURES")) x.add(new Effect("레버리지", pnl, "기초자산 변화 × 배수", "자기자본보다 큰 명목 노출이 손익을 확대합니다."));
        if (fee > 0) x.add(new Effect("비용", -fee, "금액 × 연비용 × 기간/365", "보수는 작은 비율이어도 시간이 지나며 누적됩니다."));
        if (loan > 0) x.add(new Effect("차입이자", -loan, "차입금 × 이자율 × 기간/365", "빌린 자금의 비용입니다."));
        if (x.isEmpty()) x.add(new Effect("가격 충격", pnl, "금액 × 가정한 가격 변화", "입력한 가정이 이 포지션의 평가금액에 반영됩니다."));
        return x;
    }
    private List<String> observations(ProductSpec p, Holding h, double pnl, double end) {
        List<String> x = new ArrayList<>();
        if (p.mechanism().equals("CALL") || p.mechanism().equals("PUT")) x.add("옵션 프리미엄을 처음 지불한 매수 포지션의 만기 손익입니다. 시간가치·변동성·조기행사는 제외합니다.");
        if (p.mechanism().equals("MARGIN")) x.add("신용 거래는 손실이 자기자본을 넘어설 수 있고, 실제 증권사는 담보를 사전 통지 없이 매도할 수 있습니다.");
        if (p.mechanism().equals("FUTURES")) x.add("선물은 증거금보다 큰 명목금액을 움직입니다. 실제 마진콜 시점은 일중 가격 경로와 거래소 규칙에 따라 달라집니다.");
        if (end < 0) x.add("이 입력에서는 비용을 포함한 평가 잔액이 0보다 작아졌습니다. 손실 상한으로 자르지 않은 이유는 레버리지·차입의 추가 손실을 숨기지 않기 위해서입니다.");
        if (x.isEmpty()) x.add("이 상품의 변화가 전체 포트폴리오 손익에 반영되었습니다. 다른 상품을 함께 넣어 집중도를 비교해 보세요.");
        return x;
    }
    private List<String> lessons(Portfolio p, Shocks s, List<PositionResult> positions, double profit) {
        List<String> x = new ArrayList<>();
        PositionResult largest = positions.stream().max(Comparator.comparingDouble(PositionResult::weightPercent)).orElse(null);
        if (largest != null && largest.weightPercent() >= 50) x.add(largest.label() + " 하나가 예산의 " + pct(largest.weightPercent()) + "를 차지합니다. 같은 충격이 전체 결과를 크게 좌우하는 집중 상태입니다.");
        if (positions.stream().anyMatch(v -> v.economicExposure() > v.invested()*1.01)) x.add("일부 포지션의 경제적 노출이 납입금보다 큽니다. 레버리지는 수익률을 높이는 장치가 아니라 손익과 손실 속도를 함께 확대하는 구조입니다.");
        if (positions.stream().anyMatch(v -> v.assetClass().equals("채권"))) x.add("금리 충격을 0으로 바꾸고 다시 실행하면 채권 가격 변화가 사라지는지 확인할 수 있습니다. 그 차이가 금리 민감도의 의미입니다.");
        if (positions.stream().anyMatch(v -> v.conceptSlug().equals("gold") || v.assetClass().equals("금"))) x.add("금은 주식과 동일한 원천에서 수익이 난다고 가정하지 않았습니다. 가격·환율·보관비용을 따로 확인해야 합니다.");
        x.add("이번 결과는 " + (profit >= 0 ? "이 가정에서 이익" : "이 가정에서 손실") + "일 뿐이며, 충격의 크기와 기간을 바꿔 민감도를 비교하는 것이 실험의 목적입니다.");
        return x;
    }
    private static String money(double n) { return String.format("%,.0f원", n); }
    private static String pct(double n) { return String.format("%.1f%%", n); }
}
