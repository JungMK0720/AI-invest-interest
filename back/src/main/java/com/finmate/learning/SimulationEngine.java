package com.finmate.learning;

import static com.finmate.learning.SimulationFormat.money;
import static com.finmate.learning.SimulationFormat.josa;
import static com.finmate.learning.SimulationFormat.josaRo;
import static com.finmate.learning.SimulationFormat.number;
import static com.finmate.learning.SimulationFormat.percent;
import static com.finmate.learning.SimulationFormat.readable;
import static com.finmate.learning.SimulationFormat.signedMoney;
import static com.finmate.learning.SimulationFormat.signedPercent;

import com.finmate.learning.SimulationDtos.Metric;
import com.finmate.learning.SimulationDtos.Point;
import com.finmate.learning.SimulationDtos.RunRequest;
import com.finmate.learning.SimulationDtos.RunResponse;
import com.finmate.learning.SimulationDtos.ScenarioSpec;
import com.finmate.learning.SimulationDtos.Series;
import com.finmate.learning.SimulationDtos.Table;
import com.finmate.learning.SimulationDtos.TableRow;
import com.finmate.learning.SimulationDtos.Tone;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.springframework.stereotype.Service;

/**
 * 모의투자 계산 엔진.
 *
 * <p>모든 시나리오는 순수 계산이며 외부 시세를 쓰지 않는다. 실시간 데이터를 붙이기 전까지는
 * "가정값으로 구조를 실험하는 도구"라는 성격을 분명히 하기 위해서다. 난수는 seed로 고정해
 * 같은 입력·같은 seed면 항상 같은 결과를 낸다(발표·재현 검증에 필요).
 *
 * <p>월 단위 경로는 로그정규(기하 브라운 운동)를 쓴다. 산술 평균 수익률을 그대로 곱하면
 * 손실 구간에서 원금이 음수가 될 수 있어 금융 상품의 가격 경로로 적절하지 않다.
 */
@Service
public class SimulationEngine {

    /** seed를 지정하지 않았을 때 쓰는 기본값. 화면 첫 진입 결과를 항상 동일하게 만든다. */
    private static final long DEFAULT_SEED = 20_260_909L;

    private static final int TRADING_DAYS = 252;

    private final SimulationCatalog catalog;

    public SimulationEngine(SimulationCatalog catalog) {
        this.catalog = catalog;
    }

    public RunResponse run(String scenarioId, RunRequest request) {
        ScenarioSpec spec = catalog.find(scenarioId);
        SimulationInputs in = SimulationInputs.of(spec, request == null ? null : request.inputs());
        long seed = request == null || request.seed() == null ? DEFAULT_SEED : request.seed();
        return switch (scenarioId) {
            case "price-path" -> pricePath(spec, in, seed);
            case "dca-vs-lumpsum" -> dcaVsLumpSum(spec, in, seed);
            case "bond-rate-shock" -> bondRateShock(spec, in, seed);
            case "fee-drag" -> feeDrag(spec, in, seed);
            case "etf-tracking" -> etfTracking(spec, in, seed);
            case "diversification" -> diversification(spec, in, seed);
            case "liquidity-exit" -> liquidityExit(spec, in, seed);
            case "leverage-decay" -> leverageDecay(spec, in, seed);
            case "option-payoff" -> optionPayoff(spec, in, seed);
            case "futures-margin" -> futuresMargin(spec, in, seed);
            case "deposit-inflation" -> depositInflation(spec, in, seed);
            default -> throw new IllegalArgumentException("구현되지 않은 시나리오입니다: " + scenarioId);
        };
    }

    // ---------------------------------------------------------------- 시나리오

    /** 같은 가정에서도 경로에 따라 결과가 달라진다는 것을 보여준다. */
    private RunResponse pricePath(ScenarioSpec spec, SimulationInputs in, long seed) {
        double amount = in.num("amount");
        int months = (int) in.num("months");
        double mu = in.rate("annualReturn");
        double sigma = in.rate("annualVolatility");
        boolean dca = "DCA".equals(in.text("style"));

        double[] price = geometricPath(months, mu, sigma, seed);
        double monthly = amount / months;
        double units = dca ? 0 : amount / price[0];
        double[] value = new double[months + 1];
        double[] invested = new double[months + 1];
        double investedSoFar = dca ? 0 : amount;

        for (int t = 0; t <= months; t++) {
            if (dca && t < months) {
                units += monthly / price[t];
                investedSoFar += monthly;
            }
            value[t] = units * price[t];
            invested[t] = investedSoFar;
        }

        double finalValue = value[months];
        double totalInvested = invested[months];
        double profit = finalValue - totalInvested;
        double returnRatio = profit / totalInvested;
        double mdd = maxDrawdown(value);
        double annualised = Math.pow(finalValue / totalInvested, 12.0 / months) - 1;

        List<Metric> metrics = List.of(
                metric("총 투자원금", money(totalInvested), totalInvested, "원", Tone.NEUTRAL, dca ? "매월 " + money(monthly) + "씩 나눠 넣었습니다." : "처음에 한 번에 넣었습니다."),
                metric("최종 평가금액", money(finalValue), finalValue, "원", tone(profit), months + "개월 뒤 평가액입니다."),
                metric("손익", signedMoney(profit), profit, "원", tone(profit), "세금·거래비용은 반영하지 않았습니다."),
                metric("누적 수익률", signedPercent(returnRatio), returnRatio, "%", tone(profit), "원금 대비 손익입니다."),
                metric("연환산 수익률", signedPercent(annualised), annualised, "%", tone(annualised), "가정한 기대수익률 " + percent(mu) + "와 비교해 보세요."),
                metric("최대 낙폭(MDD)", percent(-mdd), -mdd, "%", mdd > 0.2 ? Tone.BAD : Tone.NEUTRAL, "최고점 대비 가장 크게 떨어진 폭입니다."));

        List<Series> series = List.of(
                new Series("평가금액", "line", "원", pointsOfMonths(value)),
                new Series("누적 투자원금", "line", "원", pointsOfMonths(invested)));

        List<String> lessons = new ArrayList<>();
        if (profit < 0) {
            lessons.add("연 " + percent(mu) + "를 가정했지만 이 경로에서는 손실로 끝났습니다. 기대수익률은 평균값이지 약속이 아닙니다.");
        } else if (annualised < mu) {
            lessons.add("최종 수익은 났지만 연환산 " + signedPercent(annualised) + "로 가정치 " + percent(mu) + "에 못 미쳤습니다. 변동성이 클수록 실현 수익률은 평균에서 멀어집니다.");
        } else {
            lessons.add("이번 경로는 가정치보다 좋았습니다. 좋은 결과 하나를 실력으로 해석하지 않는 것이 중요합니다.");
        }
        lessons.add("도중에 최대 " + percent(mdd) + " 하락을 겪었습니다. 이 하락을 견딜 수 있는지가 위험 감수 능력의 실제 의미입니다.");
        lessons.add(dca
                ? "나눠 사면 매입단가가 평균에 가까워지지만, 계속 오르는 구간에서는 한 번에 사는 것보다 불리할 수 있습니다."
                : "한 번에 사면 진입 시점의 영향이 그대로 남습니다. ‘나눠서 매수’로 바꿔 같은 경로를 다시 보세요.");
        lessons.add("‘다른 시장 상황 보기’로 seed만 바꾸면 같은 가정에서도 결과가 달라집니다.");

        String headline = profit >= 0
                ? months + "개월 뒤 " + readable(finalValue) + " · 원금 대비 " + signedPercent(returnRatio)
                : months + "개월 뒤 " + readable(finalValue) + josaRo(readable(finalValue)) + " "
                        + readable(Math.abs(profit)) + " 손실";

        return response(spec, headline, metrics, series, List.of(), lessons,
                List.of("월 수익률은 로그정규 분포를 가정했습니다.",
                        "세금, 매매수수료, 배당은 반영하지 않았습니다.",
                        "특정 상품의 과거·미래 수익률이 아닙니다."),
                in, seed);
    }

    /** 같은 경로 위에서 거치식과 적립식을 비교한다. */
    private RunResponse dcaVsLumpSum(ScenarioSpec spec, SimulationInputs in, long seed) {
        double amount = in.num("amount");
        int months = (int) in.num("months");
        double mu = in.rate("annualReturn");
        double sigma = in.rate("annualVolatility");
        String shape = in.text("shape");

        double[] price = shapedPath(months, mu, sigma, shape, seed);
        double monthly = amount / months;

        double lumpUnits = amount / price[0];
        double dcaUnits = 0;
        double[] lumpValue = new double[months + 1];
        double[] dcaValue = new double[months + 1];
        for (int t = 0; t <= months; t++) {
            if (t < months) {
                dcaUnits += monthly / price[t];
            }
            lumpValue[t] = lumpUnits * price[t];
            dcaValue[t] = dcaUnits * price[t];
        }

        double lumpFinal = lumpValue[months];
        double dcaFinal = dcaValue[months];
        double lumpAvgPrice = price[0];
        double dcaAvgPrice = amount / dcaUnits;
        double gap = dcaFinal - lumpFinal;

        List<Metric> metrics = List.of(
                metric("거치식 최종 평가금액", money(lumpFinal), lumpFinal, "원", tone(lumpFinal - amount), "처음에 전액을 넣은 경우입니다."),
                metric("적립식 최종 평가금액", money(dcaFinal), dcaFinal, "원", tone(dcaFinal - amount), "매월 " + money(monthly) + "씩 넣은 경우입니다."),
                metric("두 방식의 차이", signedMoney(gap), gap, "원", tone(gap), gap >= 0 ? "이 경로에서는 적립식이 유리했습니다." : "이 경로에서는 거치식이 유리했습니다."),
                metric("거치식 매입단가", number(lumpAvgPrice, 2), lumpAvgPrice, "p", Tone.NEUTRAL, "첫 달 가격 그대로입니다."),
                metric("적립식 평균 매입단가", number(dcaAvgPrice, 2), dcaAvgPrice, "p", Tone.NEUTRAL, "매월 매수 가격의 조화평균입니다."),
                metric("기간 중 최저 가격", number(min(price), 2), min(price), "p", Tone.NEUTRAL, "가격 경로의 바닥입니다."));

        List<Series> series = List.of(
                new Series("가격 지수", "line", "p", pointsOfMonths(price)),
                new Series("거치식 평가금액", "line", "원", pointsOfMonths(lumpValue)),
                new Series("적립식 평가금액", "line", "원", pointsOfMonths(dcaValue)));

        List<String> lessons = new ArrayList<>();
        lessons.add(gap >= 0
                ? "먼저 떨어졌다가 회복하는 구간에서는 나눠 사는 쪽이 더 낮은 단가를 만듭니다."
                : "꾸준히 오르는 구간에서는 일찍 넣은 돈이 더 오래 일합니다. 적립식이 항상 유리한 것은 아닙니다.");
        lessons.add("적립식의 장점은 수익률이 아니라, 진입 시점 하나에 결과가 크게 좌우되지 않게 만드는 것입니다.");
        lessons.add("‘시장 흐름 가정’을 바꿔 네 가지 경우를 모두 보면 어떤 조건에서 어느 방식이 유리한지 규칙이 보입니다.");

        String headline = gap >= 0
                ? "이 흐름에서는 적립식이 " + readable(Math.abs(gap)) + " 앞섰습니다"
                : "이 흐름에서는 거치식이 " + readable(Math.abs(gap)) + " 앞섰습니다";

        return response(spec, headline, metrics, series, List.of(), lessons,
                List.of("두 방식에 같은 총액과 같은 가격 경로를 적용했습니다.",
                        "적립식의 미투자 잔액에 대한 이자는 반영하지 않았습니다.",
                        "세금과 매매비용은 반영하지 않았습니다."),
                in, seed);
    }

    /** 채권 가격을 현금흐름 할인으로 직접 계산한다. */
    private RunResponse bondRateShock(ScenarioSpec spec, SimulationInputs in, long seed) {
        double face = in.num("faceAmount");
        double couponRate = in.rate("couponRate");
        int n = (int) in.num("maturityYears");
        double y0 = in.rate("marketYield");
        double dy = in.bp("yieldChangeBp");
        boolean hold = "HOLD".equals(in.text("plan"));

        double coupon = face * couponRate;
        double p0 = bondPrice(face, coupon, n, y0);
        double y1 = Math.max(0.0001, y0 + dy);
        double p1 = bondPrice(face, coupon, n, y1);
        double macaulay = macaulayDuration(face, coupon, n, y0, p0);
        double modified = macaulay / (1 + y0);
        double convexity = convexity(face, coupon, n, y0, p0);
        double approx = -modified * dy + 0.5 * convexity * dy * dy;
        double actual = (p1 - p0) / p0;
        double sellPnl = p1 - p0;
        double holdTotal = coupon * n + face;
        double holdPnl = holdTotal - p0;

        List<Metric> metrics = new ArrayList<>(List.of(
                metric("현재 채권 가격", money(p0), p0, "원", Tone.NEUTRAL, "액면 " + money(face) + " 대비 " + percent(p0 / face) + " 수준입니다."),
                metric("금리 변동 후 가격", money(p1), p1, "원", tone(sellPnl), "시장금리 " + percent(y0) + " → " + percent(y1)),
                metric("가격 변화율", signedPercent(actual), actual, "%", tone(sellPnl), "금리와 가격은 반대로 움직입니다."),
                metric("수정 듀레이션", number(modified, 2), modified, "년", Tone.NEUTRAL, "금리 1%p 변동 시 대략 " + number(modified, 2) + "% 가격이 움직입니다."),
                metric("볼록성", number(convexity, 1), convexity, "", Tone.NEUTRAL, "듀레이션만으로 설명되지 않는 곡률입니다."),
                metric("듀레이션 근사 예측", signedPercent(approx), approx, "%", Tone.NEUTRAL, "실제 " + signedPercent(actual) + "와의 차이가 볼록성의 몫입니다.")));
        metrics.add(hold
                ? metric("만기 보유 시 총 수령액", money(holdTotal), holdTotal, "원", tone(holdPnl), "이자 " + money(coupon * n) + " + 액면 " + money(face))
                : metric("즉시 매도 시 손익", signedMoney(sellPnl), sellPnl, "원", tone(sellPnl), "지금 팔면 확정되는 금액입니다."));

        List<Point> curve = new ArrayList<>();
        for (int shift = -300; shift <= 300; shift += 25) {
            double y = Math.max(0.0001, y0 + shift / 10_000.0);
            curve.add(new Point((shift > 0 ? "+" : "") + shift + "bp", bondPrice(face, coupon, n, y)));
        }
        List<Point> cashflow = new ArrayList<>();
        for (int t = 1; t <= n; t++) {
            cashflow.add(new Point(t + "년", t == n ? coupon + face : coupon));
        }
        List<Series> series = List.of(
                new Series("금리별 채권 가격", "line", "원", List.copyOf(curve)),
                new Series("연도별 현금흐름", "bar", "원", List.copyOf(cashflow)));

        List<String> lessons = new ArrayList<>();
        lessons.add(dy > 0
                ? "금리가 오르면 이미 가진 채권의 이자가 상대적으로 낮아져 가격이 떨어집니다. 이번 계산에서는 " + signedMoney(sellPnl) + "입니다."
                : dy < 0 ? "금리가 내리면 기존 채권의 이자가 상대적으로 높아져 가격이 오릅니다." : "금리 변동을 0으로 두면 가격은 그대로입니다. 변동 폭을 바꿔 보세요.");
        lessons.add("만기 " + n + "년 채권의 수정 듀레이션은 " + number(modified, 2) + "입니다. 만기를 늘려 같은 금리 변동을 다시 보면 민감도가 커지는 것을 볼 수 있습니다.");
        lessons.add(hold
                ? "만기까지 보유하면 중간 가격 변동과 무관하게 이자와 액면을 받습니다. 다만 발행자가 갚지 못하는 신용위험은 남습니다."
                : "중도 매도하면 그 시점의 시장금리가 손익을 결정합니다. ‘만기까지 보유’로 바꿔 차이를 확인하세요.");
        lessons.add("채권은 원금이 보장되는 상품이 아닙니다. 신용위험과 중도 매도 시 가격위험을 함께 확인해야 합니다.");

        String headline = "금리 " + (dy >= 0 ? "+" : "") + (int) in.num("yieldChangeBp") + "bp → 채권 가격 " + signedPercent(actual)
                + " (" + signedMoney(sellPnl) + ")";

        return response(spec, headline, metrics, series, List.of(), lessons,
                List.of("연 1회 이자 지급, 이자 재투자 없음을 가정했습니다.",
                        "발행자의 부도 가능성과 세금은 반영하지 않았습니다.",
                        "실제 채권은 잔존일수·경과이자 계산이 더 복잡합니다."),
                in, seed);
    }

    /** 보수 차이가 복리로 누적되는 모습을 보여준다. */
    private RunResponse feeDrag(ScenarioSpec spec, SimulationInputs in, long seed) {
        double amount = in.num("amount");
        int years = (int) in.num("years");
        double r = in.rate("annualReturn");
        double feeA = in.rate("feeA");
        double feeB = in.rate("feeB");

        double[] valueA = new double[years + 1];
        double[] valueB = new double[years + 1];
        double[] gross = new double[years + 1];
        valueA[0] = amount;
        valueB[0] = amount;
        gross[0] = amount;
        for (int y = 1; y <= years; y++) {
            valueA[y] = valueA[y - 1] * (1 + r) * (1 - feeA);
            valueB[y] = valueB[y - 1] * (1 + r) * (1 - feeB);
            gross[y] = gross[y - 1] * (1 + r);
        }

        double gap = valueA[years] - valueB[years];
        double lostA = gross[years] - valueA[years];
        double lostB = gross[years] - valueB[years];
        double lostRatioB = lostB / gross[years];

        List<Metric> metrics = List.of(
                metric("보수 없는 경우", money(gross[years]), gross[years], "원", Tone.NEUTRAL, "비교 기준입니다."),
                metric("상품 A (" + percent(feeA) + ")", money(valueA[years]), valueA[years], "원", Tone.GOOD, "비용으로 " + money(lostA) + " 차감"),
                metric("상품 B (" + percent(feeB) + ")", money(valueB[years]), valueB[years], "원", Tone.BAD, "비용으로 " + money(lostB) + " 차감"),
                metric("두 상품의 차이", money(gap), gap, "원", Tone.BAD, "보수 " + percent(feeB - feeA) + " 차이가 " + years + "년 누적된 결과입니다."),
                metric("B가 비용으로 잃은 비중", percent(lostRatioB), lostRatioB, "%", Tone.BAD, "수익 없는 해에도 보수는 차감됩니다."),
                metric("차이의 원금 대비 비율", percent(gap / amount), gap / amount, "%", Tone.BAD, "처음 넣은 " + money(amount) + " 기준입니다."));

        List<Series> series = List.of(
                new Series("상품 A", "line", "원", pointsOfYears(valueA)),
                new Series("상품 B", "line", "원", pointsOfYears(valueB)),
                new Series("보수 없는 경우", "line", "원", pointsOfYears(gross)));

        List<TableRow> rows = new ArrayList<>();
        for (int y = 5; y <= years; y += 5) {
            rows.add(new TableRow(y + "년", List.of(money(valueA[y]), money(valueB[y]), money(valueA[y] - valueB[y]))));
        }
        if (rows.isEmpty() || years % 5 != 0) {
            rows.add(new TableRow(years + "년", List.of(money(valueA[years]), money(valueB[years]), money(gap))));
        }
        List<Table> tables = List.of(new Table("기간별 차이", List.of("경과", "상품 A", "상품 B", "차이"), List.copyOf(rows),
                "보수는 수익률과 무관하게 매년 순자산에서 차감됩니다."));

        List<String> lessons = List.of(
                "총보수 " + percent(feeB - feeA) + " 차이가 " + years + "년 뒤 " + readable(gap)
                        + josa(readable(gap), "이", "가") + " 되었습니다. 비용은 수익률과 달리 확정적으로 발생합니다.",
                "수익이 나지 않은 해에도 보수는 차감됩니다. 그래서 비용은 기대수익률보다 먼저 확인해야 하는 항목입니다.",
                "상품설명서의 ‘총보수·비용비율(TER)’과 함께 판매수수료, 환매수수료, 매매·중개수수료도 확인하세요.",
                "기간을 늘려 보면 차이가 선형이 아니라 점점 빨리 벌어지는 것을 볼 수 있습니다.");

        String headline = "보수 " + percent(feeA) + " vs " + percent(feeB) + " → " + years + "년 뒤 " + readable(gap) + " 차이";

        return response(spec, headline, metrics, series, tables, lessons,
                List.of("두 상품의 세전 수익률이 같다고 가정했습니다.",
                        "보수는 매년 말 순자산에서 차감된다고 단순화했습니다.",
                        "세금과 매매수수료는 반영하지 않았습니다."),
                in, seed);
    }

    /** 지수 수익률이 계좌 수익률이 되기까지 무엇이 깎이는지 단계별로 보여준다. */
    private RunResponse etfTracking(ScenarioSpec spec, SimulationInputs in, long seed) {
        double amount = in.num("amount");
        int years = (int) in.num("years");
        double indexReturn = in.rate("indexReturn");
        double ter = in.rate("ter");
        double te = in.bp("trackingErrorBp");
        double premiumBuy = in.bp("premiumBuyBp");
        double premiumSell = in.bp("premiumSellBp");
        double halfSpread = in.bp("spreadBp") / 2;

        double indexGrowth = Math.pow(1 + indexReturn, years);
        double indexValue = amount * indexGrowth;
        double afterTer = amount * Math.pow((1 + indexReturn) * (1 - ter), years);
        double afterTe = amount * Math.pow((1 + indexReturn) * (1 - ter) * (1 + te), years);
        // 매수 시 괴리·스프레드만큼 비싸게 사면 같은 돈으로 더 적은 좌수를 갖는다.
        double buyFactor = 1 / (1 + premiumBuy + halfSpread);
        double afterBuy = afterTe * buyFactor;
        // 매도 시 괴리·스프레드는 회수 금액을 직접 깎는다.
        double sellFactor = 1 + premiumSell - halfSpread;
        double net = afterBuy * sellFactor;

        double indexRatio = indexGrowth - 1;
        double netRatio = net / amount - 1;
        double gapRatio = indexRatio - netRatio;

        List<Metric> metrics = List.of(
                metric("지수 기준 평가금액", money(indexValue), indexValue, "원", Tone.NEUTRAL, "기초지수만 따라갔을 때입니다."),
                metric("실제 손에 남는 금액", money(net), net, "원", tone(net - amount), "매수·보유·매도 비용을 모두 반영했습니다."),
                metric("지수 누적 수익률", signedPercent(indexRatio), indexRatio, "%", Tone.NEUTRAL, years + "년 누적입니다."),
                metric("내 계좌 누적 수익률", signedPercent(netRatio), netRatio, "%", tone(netRatio), "실제 체감하는 수익률입니다."),
                metric("수익률 격차", percent(gapRatio), gapRatio, "%", Tone.BAD, "보수·추적오차·괴리·스프레드의 합입니다."),
                metric("보수로 나간 금액", money(indexValue - afterTer), indexValue - afterTer, "원", Tone.BAD, "연 " + percent(ter) + " × " + years + "년"));

        List<Series> series = List.of(new Series("단계별 차감", "waterfall", "원", List.of(
                new Point("지수 기준", indexValue),
                new Point("총보수 차감", afterTer),
                new Point("추적오차 반영", afterTe),
                new Point("매수 괴리·스프레드", afterBuy),
                new Point("매도 괴리·스프레드", net))));

        List<TableRow> rows = List.of(
                new TableRow("총보수(TER)", List.of(percent(ter) + " / 년", signedMoney(afterTer - indexValue))),
                new TableRow("추적오차", List.of(number(in.num("trackingErrorBp"), 0) + "bp / 년", signedMoney(afterTe - afterTer))),
                new TableRow("매수 시 괴리·스프레드", List.of(number(in.num("premiumBuyBp") + in.num("spreadBp") / 2, 0) + "bp", signedMoney(afterBuy - afterTe))),
                new TableRow("매도 시 괴리·스프레드", List.of(number(in.num("premiumSellBp") - in.num("spreadBp") / 2, 0) + "bp", signedMoney(net - afterBuy))));
        List<Table> tables = List.of(new Table("무엇이 얼마나 깎았나", List.of("항목", "적용값", "금액 영향"), rows,
                "괴리율은 시장가격이 순자산가치(NAV)와 벌어진 정도입니다."));

        List<String> lessons = List.of(
                "지수는 " + signedPercent(indexRatio) + "였지만 계좌 기준으로는 " + signedPercent(netRatio) + "였습니다. 차이 " + percent(gapRatio) + "가 상품과 거래에서 발생한 비용입니다.",
                "보수는 보유 기간에 비례해 커지고, 괴리율과 스프레드는 매매하는 순간에 한 번씩 발생합니다. 성격이 다른 비용입니다.",
                "거래량이 적은 시간대나 기초자산 시장이 닫힌 시간에는 괴리율이 커질 수 있습니다. 매수·매도 괴리율을 크게 바꿔 보세요.",
                "‘지수를 추종한다’는 설명은 지수와 같은 수익을 준다는 뜻이 아닙니다. 추적오차와 비용을 함께 확인해야 합니다.");

        String headline = "지수 " + signedPercent(indexRatio) + " → 내 계좌 " + signedPercent(netRatio) + " (차이 " + percent(gapRatio) + ")";

        return response(spec, headline, metrics, series, tables, lessons,
                List.of("지수 수익률이 매년 동일하다고 가정했습니다.",
                        "분배금 재투자와 세금은 반영하지 않았습니다.",
                        "괴리율은 매수·매도 시점에 한 번씩만 적용했습니다."),
                in, seed);
    }

    /** 상관계수에 따라 분산 효과가 있는지를 직접 확인한다. */
    private RunResponse diversification(ScenarioSpec spec, SimulationInputs in, long seed) {
        double amount = in.num("amount");
        int months = (int) in.num("months");
        double wA = in.rate("weightA");
        double wB = 1 - wA;
        double muA = in.rate("returnA");
        double volA = in.rate("volA");
        double muB = in.rate("returnB");
        double volB = in.rate("volB");
        double rho = Math.max(-1, Math.min(1, in.num("correlation")));

        double theoreticalVol = Math.sqrt(wA * wA * volA * volA + wB * wB * volB * volB + 2 * wA * wB * rho * volA * volB);
        double weightedVol = wA * volA + wB * volB;
        double benefit = weightedVol - theoreticalVol;

        SplittableRandom random = new SplittableRandom(seed);
        double dt = 1.0 / 12;
        double[] onlyA = new double[months + 1];
        double[] onlyB = new double[months + 1];
        double[] mixed = new double[months + 1];
        onlyA[0] = amount;
        onlyB[0] = amount;
        mixed[0] = amount;
        double[] mixedReturns = new double[months];
        for (int t = 1; t <= months; t++) {
            double z1 = random.nextGaussian();
            double z2 = random.nextGaussian();
            double zA = z1;
            double zB = rho * z1 + Math.sqrt(Math.max(0, 1 - rho * rho)) * z2;
            double rA = Math.exp((muA - volA * volA / 2) * dt + volA * Math.sqrt(dt) * zA) - 1;
            double rB = Math.exp((muB - volB * volB / 2) * dt + volB * Math.sqrt(dt) * zB) - 1;
            double rP = wA * rA + wB * rB;
            mixedReturns[t - 1] = rP;
            onlyA[t] = onlyA[t - 1] * (1 + rA);
            onlyB[t] = onlyB[t - 1] * (1 + rB);
            mixed[t] = mixed[t - 1] * (1 + rP);
        }

        double realisedVol = stdev(mixedReturns) * Math.sqrt(12);
        double mddA = maxDrawdown(onlyA);
        double mddB = maxDrawdown(onlyB);
        double mddMix = maxDrawdown(mixed);

        List<Metric> metrics = List.of(
                metric("A만 보유했을 때 변동성", percent(volA), volA, "%", Tone.NEUTRAL, "가정한 연 변동성입니다."),
                metric("단순 가중평균 변동성", percent(weightedVol), weightedVol, "%", Tone.NEUTRAL, "분산 효과가 전혀 없을 때의 값입니다."),
                metric("포트폴리오 변동성", percent(theoreticalVol), theoreticalVol, "%", benefit > 0.001 ? Tone.GOOD : Tone.NEUTRAL, "상관계수 " + number(rho, 2) + " 반영"),
                metric("분산으로 줄인 변동성", percent(benefit), benefit, "%p", benefit > 0.001 ? Tone.GOOD : Tone.BAD, benefit > 0.001 ? "이만큼이 분산의 몫입니다." : "상관계수가 높아 줄어든 폭이 거의 없습니다."),
                metric("최대 낙폭 (A 단독)", percent(mddA), mddA, "%", Tone.BAD, "한 자산에 몰았을 때입니다."),
                metric("최대 낙폭 (분산)", percent(mddMix), mddMix, "%", mddMix < mddA ? Tone.GOOD : Tone.BAD, "이번 경로에서의 실제 낙폭입니다."),
                metric("최종 평가금액 (분산)", money(mixed[months]), mixed[months], "원", tone(mixed[months] - amount), "실현 변동성 " + percent(realisedVol)),
                metric("최종 평가금액 (A 단독)", money(onlyA[months]), onlyA[months], "원", tone(onlyA[months] - amount), "비교용입니다."));

        List<Series> series = List.of(
                new Series("A 100%", "line", "원", pointsOfMonths(onlyA)),
                new Series("B 100%", "line", "원", pointsOfMonths(onlyB)),
                new Series("A " + Math.round(wA * 100) + "% + B " + Math.round(wB * 100) + "%", "line", "원", pointsOfMonths(mixed)));

        List<String> lessons = new ArrayList<>();
        if (rho > 0.85) {
            lessons.add("상관계수가 " + number(rho, 2) + "로 높아 두 자산이 거의 함께 움직입니다. 종목 수를 늘려도 이런 조합은 분산 효과가 거의 없습니다.");
        } else if (rho < 0.2) {
            lessons.add("상관계수가 " + number(rho, 2) + "로 낮아 변동성이 " + percent(benefit) + " 줄었습니다. 분산의 핵심은 개수가 아니라 상관관계입니다.");
        } else {
            lessons.add("상관계수 " + number(rho, 2) + "에서 변동성이 " + percent(benefit) + " 줄었습니다. 상관계수를 1로 올려 효과가 사라지는 것을 확인해 보세요.");
        }
        lessons.add("분산은 손실을 없애지 않습니다. 이번 경로에서도 분산 포트폴리오는 최대 " + percent(mddMix) + " 하락했습니다.");
        lessons.add("같은 산업에 집중한 ETF 여러 개를 사는 것은 종목 수만 늘릴 뿐 상관계수를 낮추지 못합니다.");
        lessons.add("기대수익률은 비중의 가중평균이지만 변동성은 그렇지 않습니다. 이 비대칭이 자산배분의 근거입니다.");

        String headline = "변동성 " + percent(weightedVol) + " → " + percent(theoreticalVol) + " (상관계수 " + number(rho, 2) + ")";

        return response(spec, headline, metrics, series, List.of(), lessons,
                List.of("매월 목표 비중으로 재조정(리밸런싱)한다고 가정했습니다.",
                        "상관계수는 기간 내내 일정하다고 가정했습니다. 실제로는 하락장에서 함께 오르는 경향이 있습니다.",
                        "거래비용과 세금은 반영하지 않았습니다."),
                in, seed);
    }

    /** 현금화 비용이 보유금액과 거래대금의 관계에서 나온다는 것을 보여준다. */
    private RunResponse liquidityExit(ScenarioSpec spec, SimulationInputs in, long seed) {
        double holding = in.num("holdingAmount");
        double adv = in.num("avgDailyValue");
        double spread = in.bp("spreadBp");
        String urgency = in.text("urgency");
        int days = switch (urgency) {
            case "THREE_DAYS" -> 3;
            case "TEN_DAYS" -> 10;
            default -> 1;
        };

        double dailySell = holding / days;
        double participation = dailySell / adv;
        // 시장충격 근사: 참여율의 제곱근에 비례. 참여율 10%에서 약 32bp가 되도록 계수를 잡았다.
        double impact = Math.sqrt(participation) * 0.01;
        double costRatio = spread / 2 + impact;
        double cost = holding * costRatio;
        double received = holding - cost;

        List<Metric> metrics = List.of(
                metric("하루 매도 금액", money(dailySell), dailySell, "원", Tone.NEUTRAL, days + "일에 나눠 팔 때의 하루치입니다."),
                metric("일평균 거래대금 대비 참여율", percent(participation), participation, "%", participation > 0.2 ? Tone.BAD : Tone.NEUTRAL, "시장 하루 거래의 몇 %를 내가 차지하는지입니다."),
                metric("스프레드 비용", percent(spread / 2), spread / 2, "%", Tone.BAD, "사자·팔자 호가 차이의 절반을 부담합니다."),
                metric("시장충격 비용", percent(impact), impact, "%", impact > 0.005 ? Tone.BAD : Tone.NEUTRAL, "내 매도가 가격을 밀어내는 몫입니다."),
                metric("총 현금화 비용", money(cost), cost, "원", Tone.BAD, "합계 " + percent(costRatio) + "입니다."),
                metric("실제 손에 들어오는 금액", money(received), received, "원", Tone.NEUTRAL, "표시 가격이 아니라 이 금액이 현금입니다."),
                metric("현금화 소요일", days + "일", days, "일", days > 1 ? Tone.NEUTRAL : Tone.BAD, days == 1 ? "오늘 안에 다 팔면 비용이 커집니다." : "나눠 팔면 비용은 줄지만 그동안 가격이 변합니다."));

        List<Point> comparison = new ArrayList<>();
        for (int d : new int[] {1, 3, 5, 10, 20}) {
            double p = (holding / d) / adv;
            comparison.add(new Point(d + "일", holding * (spread / 2 + Math.sqrt(p) * 0.01)));
        }
        List<Series> series = List.of(new Series("매도 기간별 예상 비용", "bar", "원", List.copyOf(comparison)));

        List<TableRow> rows = new ArrayList<>();
        for (int d = 1; d <= days; d++) {
            rows.add(new TableRow(d + "일차", List.of(money(dailySell), percent(participation), money(dailySell * costRatio))));
        }
        List<Table> tables = List.of(new Table("일자별 매도 계획", List.of("일자", "매도 금액", "참여율", "예상 비용"), List.copyOf(rows),
                "환매형 상품(펀드 등)은 여기에 환매 대금 지급까지의 영업일이 더 붙습니다."));

        List<String> lessons = new ArrayList<>();
        if (participation > 0.3) {
            lessons.add("하루 거래대금의 " + percent(participation) + "를 혼자 팔려고 합니다. 이 정도면 내 주문이 가격을 직접 끌어내립니다.");
        } else if (participation > 0.05) {
            lessons.add("참여율 " + percent(participation) + "입니다. 급하지 않다면 나눠 파는 것만으로 비용을 줄일 수 있습니다.");
        } else {
            lessons.add("참여율 " + percent(participation) + "로 충분히 유동적입니다. 다만 거래대금이 적은 종목으로 바꿔 같은 금액을 팔아 보세요.");
        }
        lessons.add("‘팔 수 있다’와 ‘원하는 가격에 팔 수 있다’는 다릅니다. 화면의 호가는 소액 기준입니다.");
        lessons.add("유동성 비용은 평상시에 작고 시장이 급락할 때 커집니다. 급하게 팔아야 하는 자금은 애초에 이런 자산에 두지 않는 편이 안전합니다.");
        lessons.add("펀드·리츠·비상장 자산은 환매기간과 환매수수료가 따로 있습니다. 상품설명서의 환매 조건을 먼저 확인하세요.");

        String headline = readable(holding) + josa(readable(holding), "을", "를") + " "
                + (days == 1 ? "오늘 전량" : days + "일에 나눠") + " 팔면 약 " + readable(cost)
                + " (" + percent(costRatio) + ") 비용";

        return response(spec, headline, metrics, series, tables, lessons,
                List.of("시장충격은 참여율의 제곱근에 비례하는 단순 근사식을 썼습니다.",
                        "매도 기간 중 가격 변동은 반영하지 않았습니다.",
                        "실제 비용은 종목·시간대·시장 상황에 따라 크게 달라집니다."),
                in, seed);
    }

    /** 일별 재조정 레버리지가 왜 배수 그대로 가지 않는지를 보여준다. */
    private RunResponse leverageDecay(ScenarioSpec spec, SimulationInputs in, long seed) {
        double amount = in.num("amount");
        int days = (int) in.num("days");
        double multiple = in.num("multiple");
        double mu = in.rate("annualReturn");
        double sigma = in.rate("annualVolatility");

        SplittableRandom random = new SplittableRandom(seed);
        double dt = 1.0 / TRADING_DAYS;
        double[] index = new double[days + 1];
        double[] levered = new double[days + 1];
        index[0] = amount;
        levered[0] = amount;
        boolean liquidated = false;
        int liquidationDay = -1;

        for (int t = 1; t <= days; t++) {
            double z = random.nextGaussian();
            double daily = Math.exp((mu - sigma * sigma / 2) * dt + sigma * Math.sqrt(dt) * z) - 1;
            index[t] = index[t - 1] * (1 + daily);
            if (liquidated) {
                levered[t] = 0;
                continue;
            }
            double leveredDaily = multiple * daily;
            double next = levered[t - 1] * (1 + leveredDaily);
            if (next <= 0) {
                liquidated = true;
                liquidationDay = t;
                levered[t] = 0;
            } else {
                levered[t] = next;
            }
        }

        double indexReturn = index[days] / amount - 1;
        double leveredReturn = levered[days] / amount - 1;
        double naive = multiple * indexReturn;
        double decay = leveredReturn - naive;
        double theoreticalDecay = -0.5 * multiple * (multiple - 1) * sigma * sigma * (days / (double) TRADING_DAYS);

        List<Metric> metrics = List.of(
                metric("지수 누적 수익률", signedPercent(indexReturn), indexReturn, "%", tone(indexReturn), days + "거래일 결과입니다."),
                metric("단순 기대 (지수 × " + number(multiple, 0) + "배)", signedPercent(naive), naive, "%", Tone.NEUTRAL, "많은 사람이 이렇게 기대합니다."),
                metric("실제 레버리지 수익률", signedPercent(leveredReturn), leveredReturn, "%", tone(leveredReturn), liquidated ? liquidationDay + "일차에 전액 손실" : "일별 재조정 결과입니다."),
                metric("차이 (변동성 손실)", signedPercent(decay), decay, "%", decay < 0 ? Tone.BAD : Tone.GOOD, "기대와 실제의 격차입니다."),
                metric("이론적 변동성 손실", signedPercent(theoreticalDecay), theoreticalDecay, "%", Tone.NEUTRAL, "-½·m·(m-1)·σ²·T 근사값입니다."),
                metric("최종 평가금액", money(levered[days]), levered[days], "원", tone(levered[days] - amount), "원금 " + money(amount) + " 대비"),
                metric("최대 낙폭", percent(maxDrawdown(levered)), maxDrawdown(levered), "%", Tone.BAD, "레버리지는 낙폭도 배수로 커집니다."));

        List<Series> series = List.of(
                new Series("지수 (1배)", "line", "원", pointsOfDays(index)),
                new Series(number(multiple, 0) + "배 레버리지", "line", "원", pointsOfDays(levered)));

        List<String> lessons = new ArrayList<>();
        if (liquidated) {
            lessons.add(liquidationDay + "거래일에 원금이 모두 사라졌습니다. 배수가 클수록 하루의 급락만으로도 회복이 불가능해집니다.");
        } else if (decay < -0.01) {
            lessons.add("지수는 " + signedPercent(indexReturn) + "인데 " + number(multiple, 0) + "배 상품은 " + signedPercent(leveredReturn) + "입니다. 단순 기대 " + signedPercent(naive) + "와 " + percent(Math.abs(decay)) + " 차이가 났습니다.");
        } else {
            lessons.add("이번 경로에서는 격차가 작았습니다. 변동성을 높이거나 기간을 늘려 다시 실행해 보세요.");
        }
        lessons.add("레버리지 상품은 매일 배수를 다시 맞춥니다. 그래서 오르내림이 반복되면 지수가 제자리로 와도 손실이 남습니다.");
        lessons.add("변동성 손실은 상품의 결함이 아니라 일별 재조정 구조의 결과입니다. 상품설명서에 ‘장기 보유에 부적합’이라고 적히는 이유입니다.");
        lessons.add("‘지수 연 기대수익률’을 0으로 두고 실행하면 순수한 변동성 효과만 볼 수 있습니다.");

        String headline = "지수 " + signedPercent(indexReturn) + " · " + number(multiple, 0) + "배 상품 " + signedPercent(leveredReturn)
                + " (기대 " + signedPercent(naive) + ")";

        return response(spec, headline, metrics, series, List.of(), lessons,
                List.of("매 거래일 배수를 다시 맞추는 구조를 가정했습니다.",
                        "운용보수, 이자비용, 롤오버 비용은 반영하지 않았습니다. 실제 상품은 이보다 불리합니다.",
                        "국내 레버리지 상품 매수에는 사전교육·기본예탁금 등 별도 요건이 있을 수 있습니다."),
                in, seed);
    }

    /** 옵션 매수와 매도의 손익 비대칭을 만기 손익 곡선으로 보여준다. */
    private RunResponse optionPayoff(ScenarioSpec spec, SimulationInputs in, long seed) {
        boolean call = "CALL".equals(in.text("optionType"));
        boolean buy = "BUY".equals(in.text("side"));
        double spot = in.num("spot");
        double strike = in.num("strike");
        double premium = in.num("premium");
        double qty = in.num("quantity");

        double totalPremium = premium * qty;
        double breakeven = call ? strike + premium : strike - premium;
        double intrinsicNow = call ? Math.max(spot - strike, 0) : Math.max(strike - spot, 0);
        double pnlNow = (buy ? 1 : -1) * (intrinsicNow - premium) * qty;

        double maxLoss = buy ? totalPremium : (call ? Double.POSITIVE_INFINITY : (strike - premium) * qty);
        double maxProfit = buy ? (call ? Double.POSITIVE_INFINITY : (strike - premium) * qty) : totalPremium;

        List<Point> curve = new ArrayList<>();
        double low = strike * 0.6;
        double high = strike * 1.4;
        int steps = 40;
        for (int i = 0; i <= steps; i++) {
            double s = low + (high - low) * i / steps;
            double intrinsic = call ? Math.max(s - strike, 0) : Math.max(strike - s, 0);
            curve.add(new Point(number(s, 0), (buy ? 1 : -1) * (intrinsic - premium) * qty));
        }

        List<Metric> metrics = List.of(
                metric("지불/수취 프리미엄", (buy ? "-" : "+") + money(totalPremium), buy ? -totalPremium : totalPremium, "원", buy ? Tone.BAD : Tone.GOOD, buy ? "권리를 사면서 먼저 나가는 돈입니다." : "권리를 팔면서 먼저 받는 돈입니다."),
                metric("손익분기 기초자산 가격", money(breakeven), breakeven, "원", Tone.NEUTRAL, "현재가 " + money(spot) + " 대비 " + signedPercent(breakeven / spot - 1)),
                metric("지금 만기라면", signedMoney(pnlNow), pnlNow, "원", tone(pnlNow), "내재가치 " + money(intrinsicNow) + " 기준입니다."),
                metric("최대 이익", Double.isInfinite(maxProfit) ? "이론상 제한 없음" : money(maxProfit), Double.isInfinite(maxProfit) ? 0 : maxProfit, "원", Tone.GOOD, buy ? "매수자의 이익 방향입니다." : "매도자의 이익은 프리미엄으로 고정됩니다."),
                metric("최대 손실", Double.isInfinite(maxLoss) ? "이론상 제한 없음" : money(maxLoss), Double.isInfinite(maxLoss) ? 0 : maxLoss, "원", Tone.BAD, buy ? "매수자는 프리미엄까지만 잃습니다." : "매도자는 손실이 프리미엄보다 훨씬 커질 수 있습니다."),
                metric("기초자산 대비 노출", money(strike * qty), strike * qty, "원", Tone.NEUTRAL, "프리미엄 " + money(totalPremium) + "으로 이만큼의 가격 변동에 노출됩니다."));

        List<Series> series = List.of(new Series("만기 손익", "line", "원", List.copyOf(curve)));

        List<String> lessons = new ArrayList<>();
        lessons.add(buy
                ? "매수자는 손실이 프리미엄 " + money(totalPremium) + "으로 제한되지만, 가격이 움직이지 않으면 그 돈을 전부 잃습니다."
                : "매도자는 이익이 프리미엄 " + money(totalPremium) + "으로 고정되고 손실은 " + (call ? "이론상 제한이 없습니다." : strike + "원까지 커질 수 있습니다."));
        lessons.add("손익분기점은 " + money(breakeven) + "입니다. 방향을 맞혀도 이 가격을 넘지 못하면 손실입니다.");
        lessons.add("옵션 가격에는 시간가치가 들어 있고, 만기에 가까워질수록 줄어듭니다. 이 곡선은 시간가치가 0이 된 만기 시점만 그린 것입니다.");
        lessons.add("‘포지션’을 매도로 바꿔 같은 조건의 손익 곡선을 비교해 보세요. 곡선이 뒤집히는 것이 두 자리의 위험 차이입니다.");

        String headline = (call ? "콜" : "풋") + " " + (buy ? "매수" : "매도") + " · 손익분기 " + money(breakeven)
                + " · " + (buy ? "최대 손실 " + money(totalPremium) : "최대 이익 " + money(totalPremium));

        return response(spec, headline, metrics, series, List.of(), lessons,
                List.of("만기 시점의 손익만 계산했습니다. 만기 전 가격은 시간가치·변동성에 따라 다릅니다.",
                        "1계약 = 기초자산 1단위로 단순화했습니다. 실제 계약 단위는 상품마다 다릅니다.",
                        "수수료, 증거금 이자, 세금은 반영하지 않았습니다."),
                in, seed);
    }

    /** 증거금이 부족해지는 시점을 일일정산으로 찾는다. */
    private RunResponse futuresMargin(ScenarioSpec spec, SimulationInputs in, long seed) {
        double deposit = in.num("deposit");
        double entryIndex = in.num("indexLevel");
        double multiplier = in.num("multiplier");
        int contracts = (int) in.num("contracts");
        double initialRate = in.rate("initialMarginRate");
        double maintenanceRate = in.rate("maintenanceMarginRate");
        int days = (int) in.num("days");
        double sigma = in.rate("annualVolatility");

        double notional = entryIndex * multiplier * contracts;
        double initialMargin = notional * initialRate;
        double leverage = notional / deposit;

        SplittableRandom random = new SplittableRandom(seed);
        double dt = 1.0 / TRADING_DAYS;
        double index = entryIndex;
        double equity = deposit;
        double minEquity = deposit;
        int marginCallDay = -1;
        List<Point> equityPoints = new ArrayList<>();
        List<Point> indexPoints = new ArrayList<>();
        List<TableRow> rows = new ArrayList<>();
        equityPoints.add(new Point("0일", equity));
        indexPoints.add(new Point("0일", index));

        for (int t = 1; t <= days; t++) {
            double z = random.nextGaussian();
            double daily = Math.exp(-sigma * sigma / 2 * dt + sigma * Math.sqrt(dt) * z) - 1;
            double previous = index;
            index = index * (1 + daily);
            double pnl = (index - previous) * multiplier * contracts;
            equity += pnl;
            minEquity = Math.min(minEquity, equity);
            double maintenance = index * multiplier * contracts * maintenanceRate;
            if (marginCallDay < 0 && equity < maintenance) {
                marginCallDay = t;
            }
            equityPoints.add(new Point(t + "일", equity));
            indexPoints.add(new Point(t + "일", index));
            if (t <= 10 || t == days || t == marginCallDay) {
                rows.add(new TableRow(t + "일차", List.of(
                        number(index, 2), signedMoney(pnl), money(Math.max(equity, 0)), money(maintenance),
                        equity < maintenance ? "추가 납입 필요" : "유지")));
            }
        }

        double totalPnl = equity - deposit;
        double indexChange = index / entryIndex - 1;

        List<Metric> metrics = List.of(
                metric("계약 명목금액", money(notional), notional, "원", Tone.NEUTRAL, contracts + "계약 × " + number(multiplier, 0) + "원 × " + number(entryIndex, 0) + "p"),
                metric("실효 레버리지", number(leverage, 2) + "배", leverage, "배", leverage > 5 ? Tone.BAD : Tone.NEUTRAL, "예탁금 " + money(deposit) + " 대비 노출입니다."),
                metric("개시증거금", money(initialMargin), initialMargin, "원", Tone.NEUTRAL, "진입에 필요한 금액입니다."),
                metric("지수 변동", signedPercent(indexChange), indexChange, "%", tone(indexChange), number(entryIndex, 2) + "p → " + number(index, 2) + "p"),
                metric("최종 손익", signedMoney(totalPnl), totalPnl, "원", tone(totalPnl), "지수 변동 " + signedPercent(indexChange) + "이 예탁금 대비 " + signedPercent(totalPnl / deposit) + "가 되었습니다."),
                metric("최저 예탁금 잔액", money(Math.max(minEquity, 0)), minEquity, "원", minEquity < initialMargin ? Tone.BAD : Tone.NEUTRAL, "기간 중 가장 낮았던 지점입니다."),
                metric("추가 납입 발생", marginCallDay > 0 ? marginCallDay + "일차" : "없음", marginCallDay, "", marginCallDay > 0 ? Tone.BAD : Tone.GOOD,
                        marginCallDay > 0 ? "이 시점에 돈을 더 넣지 못하면 반대매매됩니다." : "이번 경로에서는 유지증거금을 지켰습니다."));

        List<Series> series = List.of(
                new Series("예탁금 잔액", "line", "원", List.copyOf(equityPoints)),
                new Series("지수", "line", "p", List.copyOf(indexPoints)));

        List<Table> tables = List.of(new Table("일일정산 내역",
                List.of("일자", "지수", "정산손익", "예탁금", "유지증거금", "상태"), List.copyOf(rows),
                "선물은 만기까지 기다리는 상품이 아니라 매일 손익이 현금으로 정산되는 계약입니다."));

        List<String> lessons = new ArrayList<>();
        lessons.add("지수는 " + signedPercent(indexChange) + " 움직였는데 예탁금은 " + signedPercent(totalPnl / deposit) + " 변했습니다. 실효 레버리지 " + number(leverage, 2) + "배가 만든 차이입니다.");
        if (marginCallDay > 0) {
            lessons.add(marginCallDay + "일차에 예탁금이 유지증거금 아래로 내려갔습니다. 이때 추가 납입을 하지 못하면 손실이 확정된 채로 청산됩니다.");
        } else {
            lessons.add("이번 경로에서는 마진콜이 없었습니다. 계약 수를 늘리거나 변동성을 높여 언제 무너지는지 확인해 보세요.");
        }
        lessons.add("선물의 위험은 ‘방향을 틀리는 것’보다 ‘버티지 못하고 청산되는 것’에서 옵니다. 방향이 맞아도 중간에 청산되면 끝입니다.");
        lessons.add("증거금은 보증금이지 손실의 상한이 아닙니다. 손실은 예탁금을 넘어설 수 있습니다.");

        String headline = marginCallDay > 0
                ? marginCallDay + "일차 추가 납입 발생 · 최종 " + signedMoney(totalPnl)
                : days + "거래일 유지 · 최종 " + signedMoney(totalPnl);

        return response(spec, headline, metrics, series, tables, lessons,
                List.of("기대수익률 0%의 변동만 가정했습니다. 방향성은 넣지 않았습니다.",
                        "수수료, 세금, 롤오버 비용은 반영하지 않았습니다.",
                        "실제 증거금률과 반대매매 규정은 거래소·증권사마다 다릅니다."),
                in, seed);
    }

    /** 원금 보전과 구매력 유지가 다른 문제라는 것을 보여준다. */
    private RunResponse depositInflation(ScenarioSpec spec, SimulationInputs in, long seed) {
        double amount = in.num("amount");
        int years = (int) in.num("years");
        double rate = in.rate("depositRate");
        double tax = in.rate("taxRate");
        double inflation = in.rate("inflationRate");

        double afterTaxRate = rate * (1 - tax);
        double[] nominal = new double[years + 1];
        double[] afterTax = new double[years + 1];
        double[] needed = new double[years + 1];
        nominal[0] = amount;
        afterTax[0] = amount;
        needed[0] = amount;
        for (int y = 1; y <= years; y++) {
            nominal[y] = nominal[y - 1] * (1 + rate);
            afterTax[y] = afterTax[y - 1] * (1 + afterTaxRate);
            needed[y] = needed[y - 1] * (1 + inflation);
        }
        double real = afterTax[years] / Math.pow(1 + inflation, years);
        double realChange = real / amount - 1;
        double realRate = (1 + afterTaxRate) / (1 + inflation) - 1;

        List<Metric> metrics = List.of(
                metric("세전 원리금", money(nominal[years]), nominal[years], "원", Tone.NEUTRAL, "연 " + percent(rate) + " 복리 기준입니다."),
                metric("세후 원리금", money(afterTax[years]), afterTax[years], "원", Tone.NEUTRAL, "이자소득세 " + percent(tax) + " 차감 후입니다."),
                metric("세후 실효금리", percent(afterTaxRate), afterTaxRate, "%", Tone.NEUTRAL, "실제로 손에 남는 이자율입니다."),
                metric("같은 물건을 사려면 필요한 돈", money(needed[years]), needed[years], "원", Tone.NEUTRAL, "물가 연 " + percent(inflation) + " 기준입니다."),
                metric("구매력 기준 실질 금액", money(real), real, "원", tone(realChange), "오늘의 " + money(amount) + "와 비교한 값입니다."),
                metric("실질 구매력 변화", signedPercent(realChange), realChange, "%", tone(realChange), "연 실질금리 " + signedPercent(realRate)));

        List<Series> series = List.of(
                new Series("세후 원리금", "line", "원", pointsOfYears(afterTax)),
                new Series("물가 기준 필요 금액", "line", "원", pointsOfYears(needed)));

        List<String> lessons = new ArrayList<>();
        if (realChange < 0) {
            lessons.add("숫자는 " + money(afterTax[years]) + "로 늘었지만 살 수 있는 양은 " + percent(Math.abs(realChange)) + " 줄었습니다. 원금 보전과 구매력 유지는 다른 문제입니다.");
        } else {
            lessons.add("이 조건에서는 구매력이 " + signedPercent(realChange) + " 유지·증가했습니다. 물가상승률을 올려 언제 역전되는지 확인해 보세요.");
        }
        lessons.add("예금의 위험은 ‘원금을 잃을 위험’이 아니라 ‘물가를 못 따라갈 위험’입니다. 위험이 없는 것이 아니라 위험의 종류가 다릅니다.");
        lessons.add("예금자보호 한도를 넘는 금액은 금융회사가 파산하면 전액을 돌려받지 못할 수 있습니다. 한도는 금융회사별로 적용됩니다.");
        lessons.add("단기간에 써야 할 돈은 이 계산과 무관하게 예금·현금성 자산이 맞습니다. 목적자금의 시점이 먼저입니다.");

        String headline = years + "년 뒤 " + money(afterTax[years]) + " · 구매력 기준 " + signedPercent(realChange);

        return response(spec, headline, metrics, series, List.of(), lessons,
                List.of("이자는 연 복리, 세금은 매년 과세를 가정했습니다.",
                        "물가상승률이 기간 내내 일정하다고 가정했습니다.",
                        "예금자보호 한도와 금융회사별 적용 여부는 반영하지 않았습니다."),
                in, seed);
    }

    // ---------------------------------------------------------------- 공통 계산

    /** 기하 브라운 운동 월별 경로. price[0]=100 기준 지수로 만든다. */
    private static double[] geometricPath(int months, double mu, double sigma, long seed) {
        SplittableRandom random = new SplittableRandom(seed);
        double dt = 1.0 / 12;
        double[] path = new double[months + 1];
        path[0] = 100;
        for (int t = 1; t <= months; t++) {
            double z = random.nextGaussian();
            path[t] = path[t - 1] * Math.exp((mu - sigma * sigma / 2) * dt + sigma * Math.sqrt(dt) * z);
        }
        return path;
    }

    /**
     * 시장 흐름을 지정한 경로. 무작위 성분은 남기되 추세를 덮어씌운다.
     * 교육 목적상 "어떤 흐름에서 어떤 전략이 유리한가"를 재현 가능하게 보여주기 위한 장치다.
     */
    private static double[] shapedPath(int months, double mu, double sigma, String shape, long seed) {
        double[] base = geometricPath(months, "RANDOM".equals(shape) ? mu : 0, sigma * ("RANDOM".equals(shape) ? 1 : 0.4), seed);
        if ("RANDOM".equals(shape)) {
            return base;
        }
        double[] path = new double[months + 1];
        for (int t = 0; t <= months; t++) {
            double progress = t / (double) months;
            double trend = switch (shape) {
                case "RISING" -> 1 + 0.35 * progress;
                case "FALLING" -> 1 - 0.30 * progress;
                // 중간에 가장 깊이 떨어졌다가 시작 수준으로 돌아오는 U자 경로
                case "DIP_RECOVER" -> 1 - 0.35 * Math.sin(Math.PI * progress);
                default -> 1;
            };
            path[t] = base[t] * trend;
        }
        return path;
    }

    private static double bondPrice(double face, double coupon, int years, double yield) {
        double price = 0;
        for (int t = 1; t <= years; t++) {
            price += coupon / Math.pow(1 + yield, t);
        }
        return price + face / Math.pow(1 + yield, years);
    }

    private static double macaulayDuration(double face, double coupon, int years, double yield, double price) {
        double weighted = 0;
        for (int t = 1; t <= years; t++) {
            weighted += t * coupon / Math.pow(1 + yield, t);
        }
        weighted += years * face / Math.pow(1 + yield, years);
        return weighted / price;
    }

    private static double convexity(double face, double coupon, int years, double yield, double price) {
        double sum = 0;
        for (int t = 1; t <= years; t++) {
            sum += t * (t + 1) * coupon / Math.pow(1 + yield, t + 2);
        }
        sum += years * (years + 1) * face / Math.pow(1 + yield, years + 2);
        return sum / price;
    }

    /** 최고점 대비 최대 하락폭. 0.25면 25% 하락을 겪었다는 뜻이다. */
    private static double maxDrawdown(double[] values) {
        double peak = Double.NEGATIVE_INFINITY;
        double worst = 0;
        for (double value : values) {
            peak = Math.max(peak, value);
            if (peak > 0) {
                worst = Math.max(worst, (peak - value) / peak);
            }
        }
        return worst;
    }

    private static double stdev(double[] values) {
        if (values.length < 2) {
            return 0;
        }
        double mean = 0;
        for (double value : values) {
            mean += value;
        }
        mean /= values.length;
        double sum = 0;
        for (double value : values) {
            sum += (value - mean) * (value - mean);
        }
        return Math.sqrt(sum / (values.length - 1));
    }

    private static double min(double[] values) {
        double result = Double.POSITIVE_INFINITY;
        for (double value : values) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static List<Point> pointsOfMonths(double[] values) {
        return points(values, "개월");
    }

    private static List<Point> pointsOfYears(double[] values) {
        return points(values, "년");
    }

    private static List<Point> pointsOfDays(double[] values) {
        return points(values, "일");
    }

    /** 경로가 길면 화면과 응답 크기를 위해 최대 120개로 솎아낸다. 마지막 값은 항상 남긴다. */
    private static List<Point> points(double[] values, String unit) {
        int step = Math.max(1, values.length / 120);
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < values.length; i += step) {
            points.add(new Point(i + unit, round(values[i])));
        }
        int last = values.length - 1;
        if ((last % step) != 0) {
            points.add(new Point(last + unit, round(values[last])));
        }
        return List.copyOf(points);
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static Metric metric(String label, String value, double raw, String unit, Tone tone, String note) {
        return new Metric(label, value, round(raw), unit, tone, note);
    }

    private static Tone tone(double value) {
        if (value > 0) {
            return Tone.GOOD;
        }
        return value < 0 ? Tone.BAD : Tone.NEUTRAL;
    }

    private static RunResponse response(ScenarioSpec spec, String headline, List<Metric> metrics, List<Series> series,
                                        List<Table> tables, List<String> lessons, List<String> assumptions,
                                        SimulationInputs in, long seed) {
        return new RunResponse(spec.id(), spec.title(), headline, List.copyOf(metrics), List.copyOf(series),
                List.copyOf(tables), List.copyOf(lessons), List.copyOf(assumptions), in.echo(), seed,
                SimulationDtos.SIMULATION_NOTICE);
    }
}
