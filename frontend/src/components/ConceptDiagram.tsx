/**
 * 개념별 구조 그림.
 *
 * docs/10의 학습 5단계 중 2단계(구조 그림: 돈의 흐름·위험 발생 지점·현금화 경로)에 해당한다.
 * 값을 예측하는 차트가 아니라 관계를 보여주는 도식이므로 SVG 대신 흐름 블록으로 표현한다.
 * diagramKey는 백엔드 콘텐츠가 정하고, 알 수 없는 키는 기본 위험-수익 축으로 떨어뜨린다.
 */
export default function ConceptDiagram({ kind }: { kind: string }) {
  const diagram = diagrams[kind] ?? diagrams.DEFAULT;
  return (
    <figure className="concept-diagram" aria-label={diagram.caption}>
      <div className="diagram-flow">
        {diagram.nodes.map((node, index) => (
          <span key={`${node.text}-${index}`} className={`diagram-node kind-${node.kind}`}>
            {node.text}
          </span>
        ))}
      </div>
      {diagram.gradient && <div className="diagram-gradient" aria-hidden="true" />}
      <figcaption>{diagram.caption}</figcaption>
    </figure>
  );
}

type NodeKind = "box" | "arrow" | "risk";
type Diagram = { nodes: { text: string; kind: NodeKind }[]; caption: string; gradient?: boolean };

function flow(...parts: string[]): { text: string; kind: NodeKind }[] {
  // 홀수 번째는 상자, 짝수 번째는 화살표 라벨로 번갈아 배치한다.
  return parts.map((text, index) => ({ text, kind: index % 2 === 0 ? "box" : "arrow" }));
}

function risky(nodes: { text: string; kind: NodeKind }[], warning: string) {
  return [...nodes, { text: warning, kind: "risk" as NodeKind }];
}

const diagrams: Record<string, Diagram> = {
  DEFAULT: {
    nodes: flow("낮은 위험", "→", "높은 위험"),
    caption: "수익 가능성과 손실 가능성은 같은 축의 양쪽입니다.",
    gradient: true,
  },
  FOUNDATION: {
    nodes: flow("위험", "·", "기간", "·", "분산", "·", "비용", "·", "유동성"),
    caption: "어떤 상품이든 이 다섯 축으로 옮겨 적으면 비교할 수 있습니다.",
  },
  RISK: {
    nodes: flow("낮은 위험", "→", "높은 위험"),
    caption: "기대수익이 커진다는 것은 결과의 폭이 넓어진다는 뜻입니다.",
    gradient: true,
  },
  VOLATILITY: {
    nodes: risky(flow("평균 수익률", "±", "변동 폭"), "-50% 뒤 +50% = -25%"),
    caption: "변동성은 위아래 양쪽의 흩어짐이며 복리 결과를 깎습니다.",
  },
  DRAWDOWN: {
    nodes: risky(flow("고점", "↓ 하락", "저점", "↑ 회복", "고점"), "-40% 회복에는 +67% 필요"),
    caption: "낙폭은 대칭이 아닙니다. 깊을수록 회복이 급격히 어려워집니다.",
  },
  RISK_PROFILE: {
    nodes: risky(flow("감수 능력(재무)", "vs", "수용도(심리)"), "낮은 쪽이 실제 한도"),
    caption: "둘이 충돌하면 낮은 쪽을 기준으로 삼습니다.",
  },
  HORIZON: {
    nodes: flow("돈이 필요한 시점", "→", "감당 가능한 위험", "→", "상품 선택"),
    caption: "사용 시점이 위험 한도를 먼저 결정합니다.",
  },
  COMPOUND: {
    nodes: flow("원금", "+수익", "늘어난 원금", "+수익", "다시 늘어난 원금"),
    caption: "복리는 수익에도 비용에도 똑같이 작동합니다.",
  },
  GOAL: {
    nodes: flow("비상자금", "·", "3년 목적자금", "·", "노후자금"),
    caption: "자금마다 시점이 다르면 위험 수준도 달라야 합니다.",
  },
  DIVERSIFICATION: {
    nodes: risky(flow("자산 A", "+", "자산 B"), "상관계수가 낮을수록 효과 ↑"),
    caption: "분산의 크기는 개수가 아니라 상관관계가 정합니다.",
  },
  CORRELATION: {
    nodes: flow("ρ = -1 반대", "|", "ρ = 0 무관", "|", "ρ = +1 동일"),
    caption: "ρ가 1에 가까울수록 나눠 담아도 효과가 없습니다.",
  },
  ALLOCATION: {
    nodes: flow("주식", "·", "채권", "·", "현금"),
    caption: "비중 결정이 종목 선택보다 앞에 있는 결정입니다.",
  },
  REBALANCE: {
    nodes: flow("목표 60:40", "→ 시간", "실제 75:25", "→ 조정", "다시 60:40"),
    caption: "가만히 두면 포트폴리오는 저절로 공격적으로 변합니다.",
  },
  FEES: {
    nodes: risky(flow("수익", "-", "보수·수수료·세금"), "수익이 0인 해에도 차감"),
    caption: "비용은 확정적으로 발생하는 유일한 항목입니다.",
  },
  TAX: {
    nodes: flow("세전 수익", "-세금", "세후 수익"),
    caption: "비교는 세후 기준으로 해야 합니다.",
  },
  SPREAD: {
    nodes: risky(flow("매수호가", "↔ 스프레드", "매도호가"), "즉시 체결의 대가"),
    caption: "호가창의 두 숫자 사이 간격이 곧 거래 비용입니다.",
  },
  IMPACT: {
    nodes: risky(flow("내 주문", "→ 호가 소진", "체결가 하락"), "참여율 ↑ → 충격 ↑"),
    caption: "큰 주문은 스스로 불리한 가격을 만듭니다.",
  },
  LIQUIDITY: {
    nodes: risky(flow("보유", "시장·환매 →", "현금"), "시간 · 거래량 · 수수료 · 가격충격"),
    caption: "팔 수 있는지와 원하는 가격에 팔 수 있는지는 다릅니다.",
  },
  REDEMPTION: {
    nodes: flow("환매 신청", "→ 기준가 적용일", "가격 확정", "→ 지급일", "입금"),
    caption: "신청과 입금 사이의 날짜가 곧 유동성 위험입니다.",
  },
  INSTRUMENTS: {
    nodes: flow("빌려주기", "·", "소유하기", "·", "모아서 굴리기", "·", "계약하기"),
    caption: "돈의 흐름 방식이 위험의 발생 지점을 정합니다.",
  },
  CASH: {
    nodes: risky(flow("예치", "이자 →", "원리금"), "물가 > 세후금리면 구매력 감소"),
    caption: "원금은 지켜져도 구매력은 줄 수 있습니다.",
  },
  PROTECTION: {
    nodes: risky(flow("금융회사", "파산 시 →", "보호 대상 상품만 한도까지"), "투자상품은 대상 아님"),
    caption: "보호 여부는 판매 장소가 아니라 상품 종류로 정해집니다.",
  },
  BOND: {
    nodes: risky(flow("투자자", "자금 →", "발행자", "이자·원금 →", "투자자"), "신용위험 · 금리위험 · 만기"),
    caption: "빌려주는 구조에서는 못 갚을 위험과 금리 위험이 함께 옵니다.",
  },
  BOND_PRICE: {
    nodes: risky(flow("시장금리 ↑", "→", "채권 가격 ↓"), "가격 = 현금흐름의 현재가치"),
    caption: "표면금리는 고정, 시장금리는 변동. 그 차이를 가격이 흡수합니다.",
  },
  DURATION: {
    nodes: flow("만기 ↑", "→", "듀레이션 ↑", "→", "금리 민감도 ↑"),
    caption: "수정 듀레이션 × 금리 변동 ≈ 가격 변동률입니다.",
  },
  CREDIT: {
    nodes: risky(flow("높은 이자", "=", "높은 부도 위험"), "금리 차이는 위험의 가격"),
    caption: "이자율 차이는 선물이 아니라 가격표입니다.",
  },
  STOCK: {
    nodes: risky(flow("투자자", "자금 →", "기업", "이익의 잔여분 →", "주주"), "청산 시 채권자 다음 순위"),
    caption: "주주는 잔여 청구권자이며 순위가 가장 낮습니다.",
  },
  VALUATION: {
    nodes: flow("실적 발표", "vs", "시장의 기대", "→", "가격 변화"),
    caption: "가격은 절대 수치가 아니라 기대와의 차이에 반응합니다.",
  },
  DIVIDEND: {
    nodes: risky(flow("기업 자산", "배당 지급 →", "주주"), "배당락으로 주가 조정"),
    caption: "배당은 새 수익이 아니라 자산의 위치 이동입니다.",
  },
  STYLE: {
    nodes: flow("대형·유동적", "↔", "중소형·비유동적"),
    caption: "규모가 작을수록 팔기 어려운 구간이 생깁니다.",
  },
  FUND: {
    nodes: risky(flow("여러 투자자", "자금 →", "펀드", "보유 →", "여러 자산"), "분산 가능성 · 보수 · 환매 위험"),
    caption: "다른 투자자의 환매가 내 결과에 영향을 줍니다.",
  },
  NAV: {
    nodes: flow("순자산", "÷ 좌수", "기준가격"),
    caption: "기준가격의 절대 수준은 저평가 여부와 무관합니다.",
  },
  ACTIVE_PASSIVE: {
    nodes: risky(flow("패시브(지수 추종)", "vs", "액티브(초과수익 시도)"), "초과수익 > 추가 보수여야 의미"),
    caption: "액티브의 가치는 보수 차감 후 지수 대비로만 확인됩니다.",
  },
  ETF: {
    nodes: risky(flow("기초지수", "복제 →", "펀드(NAV)", "거래소 →", "시장가격"), "추적오차 + 괴리율 + 스프레드"),
    caption: "ETF에는 펀드의 위험과 거래의 위험이 함께 있습니다.",
  },
  PREMIUM: {
    nodes: risky(flow("NAV", "↔ 괴리", "시장가격"), "비싸게 사면 회수되지 않음"),
    caption: "괴리율은 매매 순간에 한 번 발생하는 비용입니다.",
  },
  TRACKING: {
    nodes: risky(flow("지수", "-보수·복제오차", "NAV"), "대개 지수보다 낮은 쪽"),
    caption: "추적오차는 NAV와 지수의 차이, 괴리율은 시장가와 NAV의 차이입니다.",
  },
  REIT: {
    nodes: risky(flow("임대수익", "→", "리츠", "배당 →", "주주"), "부동산 위험 + 금리 위험"),
    caption: "리츠는 부동산이면서 동시에 금리 상품입니다.",
  },
  COMMODITY: {
    nodes: risky(flow("실물", "·", "ETF", "·", "선물"), "보관비 · 롤오버 · 환율"),
    caption: "같은 자산도 담는 방식에 따라 결과가 달라집니다.",
  },
  STRUCTURED: {
    nodes: risky(flow("조건 충족", "→ 약속된 수익", "조건 이탈", "→ 큰 손실"), "수익은 상한, 손실은 열림"),
    caption: "대부분 괜찮고 가끔 크게 잘못되는 비대칭 구조입니다.",
  },
  PENSION: {
    nodes: risky(flow("장기 납입", "→ 세제 혜택", "수령"), "중도 해지 시 혜택 회수"),
    caption: "기간이 길어 비용의 영향이 가장 큰 영역입니다.",
  },
  DERIVATIVE: {
    nodes: risky(flow("기초자산 가격", "연동 →", "계약 가치"), "손실이 원금을 넘을 수 있음"),
    caption: "파생상품은 자산이 아니라 위험을 이전하는 계약입니다.",
  },
  FUTURES: {
    nodes: risky(flow("계약 체결", "매일 정산 →", "예탁금 증감"), "유지증거금 미달 시 청산"),
    caption: "만기까지 기다리는 계약이 아니라 매일 현금이 오가는 계약입니다.",
  },
  MARGIN: {
    nodes: risky(flow("예탁금", "-손실", "유지증거금 미달", "→", "추가 납입·반대매매"), "손실이 예탁금을 넘을 수 있음"),
    caption: "증거금은 보증금이지 손실 한도가 아닙니다.",
  },
  ROLL: {
    nodes: risky(flow("근월물 만기", "→ 갈아타기", "원월물"), "콘탱고면 갈아탈 때마다 손실"),
    caption: "선물로 만든 상품은 현물 보유와 다른 결과를 냅니다.",
  },
  OPTIONS: {
    nodes: risky(flow("매수자(권리)", "프리미엄 →", "매도자(의무)"), "손실 구조가 서로 반대"),
    caption: "매수자는 손실이 잘려 있고 매도자는 열려 있습니다.",
  },
  PAYOFF: {
    nodes: flow("행사가 아래", "꺾임", "행사가 위"),
    caption: "옵션 손익은 직선이 아니라 행사가에서 꺾이는 곡선입니다.",
  },
  TIME_VALUE: {
    nodes: risky(flow("옵션 가격", "=", "내재가치 + 시간가치"), "만기 다가올수록 시간가치 → 0"),
    caption: "방향이 맞아도 시간과 변동성 때문에 손실이 날 수 있습니다.",
  },
  LEVERAGE: {
    nodes: risky(flow("자기자본", "×배수 →", "노출 규모"), "손실·청산도 배수"),
    caption: "레버리지의 첫 계산은 수익이 아니라 생존 구간입니다.",
  },
  DECAY: {
    nodes: risky(flow("지수 +10%", "→ -9.09%", "지수 제자리"), "2배 상품은 -1.8%"),
    caption: "일별 재조정 때문에 흔들리기만 해도 손실이 남습니다.",
  },
  LIQUIDATION: {
    nodes: risky(flow("손실 누적", "→ 증거금 부족", "강제 청산"), "이후 회복은 내 것이 아님"),
    caption: "청산 시점은 내가 고를 수 없습니다.",
  },
  SUITABILITY: {
    nodes: flow("답변", "→ 규칙 판정", "성향 등급", "→", "권유 가능 범위"),
    caption: "진단은 권유 범위의 기준이지 투자 허가가 아닙니다.",
  },
  DISCLOSURE: {
    nodes: flow("투자위험", "·", "보수·수수료", "·", "환매 조건", "·", "손실 조건"),
    caption: "설명서는 처음부터 읽는 문서가 아니라 네 곳을 찾아 읽는 문서입니다.",
  },
  BIAS: {
    nodes: risky(flow("손실 회피", "·", "확증 편향", "·", "군집 행동"), "규칙으로 관리할 대상"),
    caption: "편향은 없앨 수 없으므로 미리 규칙을 적어 둡니다.",
  },
  RED_FLAG: {
    nodes: risky(flow("확정 수익", "+", "원금 보장", "+", "시간 압박"), "셋이 모이면 멈춤"),
    caption: "‘좋은 조건’이 아니라 ‘성립하는 조건’인지를 봅니다.",
  },
};
