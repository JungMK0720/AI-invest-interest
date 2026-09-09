package com.finmate.learning;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 학습 카탈로그의 MVP 제공자. 운영에서는 DB의 content_revisions와 source_documents로 교체한다.
 * 원문을 LLM이 재작성해 출처를 잃지 않도록 sourceUrl/sourceName을 필수로 둔다.
 */
@Service
public class LearningCatalog {
    private final List<Concept> concepts = List.of(
            new Concept("risk-return", "위험과 수익", "수익 가능성과 손실 가능성을 함께 읽기", "모든 금융상품에는 위험이 있습니다. 일반적으로 더 큰 수익 가능성에는 더 큰 손실 가능성이 함께 따릅니다.", "RISK", "https://www.investor.gov/introduction-investing/investing-basics/investment-products"),
            new Concept("stocks", "주식", "기업의 소유권과 가격 변동", "주식은 기업의 소유권을 나타냅니다. 기업 실적과 시장 상황에 따라 가격이 오를 수도, 원금보다 낮아질 수도 있습니다.", "STOCK", "https://www.investor.gov/introduction-investing/investing-basics/investment-products/stocks"),
            new Concept("bonds", "채권", "돈을 빌려주고 이자와 원금을 받는 구조", "채권에는 발행자의 신용위험이 있고, 만기 전에 팔면 금리 변화에 따라 가격이 달라질 수 있습니다.", "BOND", "https://www.investor.gov/introduction-investing/investing-basics/investment-products/bonds-or-fixed-income-products/bonds"),
            new Concept("funds", "펀드", "여러 투자자의 돈을 모아 운용하는 구조", "펀드는 여러 자산에 투자할 수 있지만 운용보수와 환매 조건, 편입자산의 위험을 함께 확인해야 합니다.", "FUND", "https://www.investor.gov/introduction-investing/investing-basics/investment-products/mutual-funds-and-exchange-traded-funds-etfs/mutual-funds"),
            new Concept("etf", "ETF", "거래소에서 거래되는 펀드 지분", "ETF는 분산에 도움을 줄 수 있지만 시장가격과 순자산가치의 차이, 스프레드, 보수와 기초자산 위험이 있습니다.", "ETF", "https://www.investor.gov/introduction-investing/general-resources/news-alerts/alerts-bulletins/investor-bulletins-24"),
            new Concept("liquidity", "유동성", "필요한 시점에 현금화할 수 있는 정도", "팔 수 있는지와 원하는 가격·시간에 팔 수 있는지는 다릅니다. 거래량, 환매기간, 수수료와 가격충격을 확인합니다.", "LIQUIDITY", "https://www.investor.gov/introduction-investing/investing-basics/glossary/liquidity-or-marketability"),
            new Concept("fees", "비용과 수수료", "작은 비용도 장기 결과에 누적되는 구조", "매매수수료뿐 아니라 운용보수, 환매수수료, 스프레드와 세금 등 상품별 비용을 비교해야 합니다.", "FEES", "https://www.investor.gov/introduction-investing/investing-basics/investment-products/mutual-funds-and-exchange-traded-funds-etfs/mutual-funds"),
            new Concept("diversification", "분산투자", "서로 다른 위험을 나누는 방식", "여러 자산과 산업에 나누는 것이 위험을 낮추는 데 도움이 될 수 있지만, 특정 산업에 집중한 ETF 하나만으로 분산이 보장되지는 않습니다.", "DIVERSIFICATION", "https://www.investor.gov/introduction-investing/getting-started/asset-allocation"),
            new Concept("time-horizon", "투자 기간", "돈을 사용해야 하는 시점과 위험의 관계", "가까운 시점에 필요한 자금은 가격 변동과 현금화 조건을 더 엄격하게 확인해야 합니다.", "HORIZON", "https://www.finra.org/rules-guidance/notices/11-25"),
            new Concept("risk-capacity", "위험감수능력과 위험수용도", "감당할 수 있는 위험과 감정적으로 받아들일 위험의 차이", "재무적으로 감당할 수 있는 위험과 심리적으로 견딜 수 있는 위험은 다를 수 있습니다. 둘이 충돌하면 충돌 사실을 먼저 확인해야 합니다.", "RISK_PROFILE", "https://www.finra.org/sites/default/files/digital-investment-advice-report.pdf"),
            new Concept("derivatives", "파생상품", "기초자산 가격에 따라 가치가 변하는 계약", "파생상품은 기초자산의 가격·금리·환율 등에 연동됩니다. 구조가 복잡하고 손실이 빠르게 커질 수 있어 계약조건과 증거금을 확인해야 합니다.", "DERIVATIVE", "https://www.investor.gov/introduction-investing/investing-basics/investment-products"),
            new Concept("futures", "선물", "미래 시점의 거래를 현재 약속하는 계약", "선물은 만기·증거금·일일정산 구조를 이해해야 합니다. 작은 가격 변동도 증거금 대비 큰 손익으로 나타날 수 있습니다.", "FUTURES", "https://www.cmegroup.com/education/courses/introduction-to-futures.html"),
            new Concept("options", "옵션", "미래에 사고팔 권리를 거래하는 계약", "콜·풋과 행사가·만기·프리미엄을 이해해야 합니다. 매수자와 매도자의 손실 구조가 다르고 시간가치가 줄어들 수 있습니다.", "OPTIONS", "https://www.investor.gov/introduction-investing/investing-basics/investment-products"),
            new Concept("margin", "마진과 증거금", "빌린 자금 또는 계약 이행을 위해 맡기는 담보", "증거금 부족 시 추가 납입이나 강제청산이 발생할 수 있습니다. 레버리지는 수익뿐 아니라 손실도 확대합니다.", "MARGIN", "https://www.investor.gov/introduction-investing/investing-basics"),
            new Concept("leverage", "레버리지", "적은 자기자본으로 큰 포지션을 만드는 구조", "레버리지는 손익의 변동폭을 확대합니다. 원금보다 큰 손실 가능성, 이자·수수료·청산 조건을 함께 확인해야 합니다.", "LEVERAGE", "https://www.investor.gov/introduction-investing/investing-basics"),
            new Concept("real-estate", "부동산과 리츠", "실물 부동산과 부동산 투자회사의 차이", "부동산은 임대수익·공실·금리·거래비용을 살펴야 하고, 리츠는 주식처럼 거래되면서도 부동산·금리 위험을 함께 가집니다.", "REAL_ESTATE", "https://www.investor.gov/introduction-investing/investing-basics/investment-products"),
            new Concept("gold", "금 투자", "실물·금 ETF·선물의 구조 차이", "금 투자는 실물 보관비용, ETF 보수와 추적오차, 선물의 증거금과 만기 구조가 서로 다릅니다. 금 가격 외에 환율도 결과에 영향을 줄 수 있습니다.", "GOLD", "https://www.investor.gov/introduction-investing/investing-basics/investment-products"),
            new Concept("cash", "예금과 현금성 자산", "원금 안정성과 유동성을 우선하는 자산", "예금과 현금성 상품도 금리·인플레이션·예금자보호 범위를 확인해야 합니다. 투자상품과 예금의 보호 구조를 구분해야 합니다.", "CASH", "https://www.investor.gov/introduction-investing/investing-basics/save-and-invest/understand-what-it-means-invest")
    );

    public List<Concept> list() { return concepts; }
    public Concept find(String slug) { return concepts.stream().filter(c -> c.slug().equals(slug)).findFirst().orElseThrow(() -> new IllegalArgumentException("학습 개념을 찾을 수 없습니다.")); }
    public record Concept(String slug, String title, String summary, String explanation, String diagramKey, String sourceUrl) {}
}
