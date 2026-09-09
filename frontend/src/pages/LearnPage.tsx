import { useEffect, useState } from "react";
import Header from "../components/Header";
import { Link } from "react-router-dom";
import { api } from "../api/client";

type Concept = { slug: string; title: string; summary: string; explanation: string; diagramKey: string; sourceUrl: string };
const fallback: Concept[] = [
  { slug: "risk-return", title: "위험과 수익", summary: "수익 가능성과 손실 가능성을 함께 읽기", explanation: "모든 금융상품에는 위험이 있습니다. 일반적으로 더 큰 수익 가능성에는 더 큰 손실 가능성이 함께 따릅니다.", diagramKey: "RISK", sourceUrl: "https://www.investor.gov/introduction-investing/investing-basics/investment-products" },
  { slug: "stocks", title: "주식", summary: "기업의 소유권과 가격 변동", explanation: "주식은 기업의 소유권을 나타냅니다. 기업 실적과 시장 상황에 따라 가격이 오를 수도, 원금보다 낮아질 수도 있습니다.", diagramKey: "STOCK", sourceUrl: "https://www.investor.gov/introduction-investing/investing-basics/investment-products/stocks" },
  { slug: "bonds", title: "채권", summary: "돈을 빌려주고 이자와 원금을 받는 구조", explanation: "채권에는 발행자의 신용위험이 있고, 만기 전에 팔면 금리 변화에 따라 가격이 달라질 수 있습니다.", diagramKey: "BOND", sourceUrl: "https://www.investor.gov/introduction-investing/investing-basics/investment-products/bonds-or-fixed-income-products/bonds" },
  { slug: "funds", title: "펀드", summary: "여러 투자자의 돈을 모아 운용하는 구조", explanation: "펀드는 여러 자산에 투자할 수 있지만 운용보수와 환매 조건, 편입자산의 위험을 함께 확인해야 합니다.", diagramKey: "FUND", sourceUrl: "https://www.investor.gov/introduction-investing/investing-basics/investment-products/mutual-funds-and-exchange-traded-funds-etfs/mutual-funds" },
  { slug: "etf", title: "ETF", summary: "거래소에서 거래되는 펀드 지분", explanation: "ETF는 분산에 도움을 줄 수 있지만 시장가격과 순자산가치의 차이, 스프레드, 보수와 기초자산 위험이 있습니다.", diagramKey: "ETF", sourceUrl: "https://www.investor.gov/introduction-investing/general-resources/news-alerts/alerts-bulletins/investor-bulletins-24" },
  { slug: "liquidity", title: "유동성", summary: "필요한 시점에 현금화할 수 있는 정도", explanation: "팔 수 있는지와 원하는 가격·시간에 팔 수 있는지는 다릅니다. 거래량, 환매기간, 수수료와 가격충격을 확인합니다.", diagramKey: "LIQUIDITY", sourceUrl: "https://www.investor.gov/introduction-investing/investing-basics/glossary/liquidity-or-marketability" },
];

export default function LearnPage() {
  const [concepts, setConcepts] = useState<Concept[]>(fallback);
  const [selected, setSelected] = useState("risk-return");
  const [answers, setAnswers] = useState<Record<string, string>>({});
  useEffect(() => { api.getLearningConcepts().then(setConcepts).catch(() => undefined); }, []);
  const topic = concepts.find(t => t.slug === selected) ?? concepts[0];
  return <div className="app-page"><Header /><main className="journey-main">
    <span className="eyebrow">상품 기초 학습 · 출처 연결 콘텐츠</span><h1>상품을 보기 전에,<br />구조를 읽는 법부터.</h1>
    <p className="intro-copy">진단 등급을 바꾸거나 가입을 권유하는 페이지가 아닙니다. 상품의 구조·위험·비용·현금화 조건을 실제 자료와 함께 학습합니다.</p>
    <div className="learning-topics">{concepts.map(t => <button className="btn btn-secondary" aria-pressed={selected === t.slug} key={t.slug} onClick={() => setSelected(t.slug)}>{t.title}</button>)}</div>
    {topic && <><article className="card evidence-card"><h2>{topic.title}</h2><p><strong>{topic.summary}</strong></p><ConceptDiagram kind={topic.diagramKey} /><p>{topic.explanation}</p><a className="lesson-source" href={topic.sourceUrl} target="_blank" rel="noreferrer">원문 출처 열기 ↗</a></article>
    <section className="card evidence-card"><h2>확인 질문</h2><p>이 개념을 실제 상품을 살펴볼 때 어떻게 확인할까요?</p><div className="quiz-options">{["상품설명서와 위험·비용 정보를 먼저 확인한다", "최근 수익률만 보고 판단한다", "진단 등급이 안정형이면 위험이 없다고 생각한다"].map(option => <label key={option}><input type="radio" name={topic.slug} checked={answers[topic.slug] === option} onChange={() => setAnswers({...answers, [topic.slug]: option})} />{option}</label>)}</div>{answers[topic.slug] && <p className="policy-card">{answers[topic.slug].startsWith("상품설명서") ? "좋습니다. 구조·위험·비용을 확인하는 것이 출발점입니다." : "수익률 하나만으로 위험을 판단할 수 없습니다. 상품설명서와 공식 위험 정보를 함께 확인하세요."}</p>}</section></>}
    <div className="action-row"><Link className="btn btn-secondary" to="/start">진단 시작 화면</Link><Link className="btn btn-secondary" to="/history">진단 이력 보기</Link></div>
  </main></div>;
}
function ConceptDiagram({ kind }: { kind: string }) {
  if (kind === "BOND") return <div className="concept-diagram"><span>투자자</span><b>자금 →</b><span>발행자</span><b>이자·원금 →</b><span>투자자</span><small>신용위험 · 금리위험 · 만기</small></div>;
  if (kind === "ETF" || kind === "FUND") return <div className="concept-diagram"><span>여러 투자자</span><b>자금 →</b><span>펀드</span><b>보유 →</b><span>여러 자산</span><small>분산 가능성 · 보수 · 기초자산 위험</small></div>;
  if (kind === "LIQUIDITY") return <div className="concept-diagram"><span>보유</span><b>시장·환매 →</b><span>현금</span><small>시간 · 거래량 · 수수료 · 가격충격</small></div>;
  return <div className="concept-diagram"><span>낮은 위험</span><i /><span>높은 위험</span><small>수익 가능성과 손실 가능성을 함께 확인</small></div>;
}
