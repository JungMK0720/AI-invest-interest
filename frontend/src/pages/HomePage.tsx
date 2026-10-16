import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Header from "../components/Header";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import type { ModelCard } from "../api/types";

function percent(value: number | undefined) {
  return value === undefined ? "–" : `${Math.round(value * 1000) / 10}%`;
}

/**
 * 첫 화면. "무엇을 사야 하나"가 아니라 "무엇이 결과를 바꾸고 어떻게 확인하나"를 배우는 서비스임을 먼저 말한다.
 * 숫자(용어 수, 모델 지표)는 하드코딩하지 않고 API에서 받아 실제 배포 상태와 어긋나지 않게 한다.
 */
export default function HomePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [termCount, setTermCount] = useState<number | null>(null);
  const [categoryCount, setCategoryCount] = useState<number | null>(null);
  const [models, setModels] = useState<ModelCard[]>([]);

  useEffect(() => {
    let alive = true;
    api.getGlossaryCategories().then(data => {
      if (!alive) return;
      setCategoryCount(data.categories.length);
      setTermCount(data.categories.reduce((sum, category) => sum + category.termCount, 0));
    }).catch(() => undefined);
    api.getModels().then(data => { if (alive) setModels(data.card.models); }).catch(() => undefined);
    return () => { alive = false; };
  }, []);

  const style = models.find(model => model.id === "style-signal");
  const retrieval = models.find(model => model.id === "term-retrieval");

  return (
    <div className="app-page">
      <Header />
      <main className="home-main">
        <section className="home-hero">
          <div>
            <span className="eyebrow">FINMATE · 투자 성향 진단과 금융 학습</span>
            <h1>무엇을 살지보다,<br />무엇이 결과를 바꾸는지.</h1>
            <p className="intro-copy">
              FinMATE는 투자 성향을 규칙으로 진단하고, 그 근거를 설명하고, 주식 용어와 상품 구조를 배우고,
              가정한 숫자로 직접 실험해 보는 학습 서비스입니다. 특정 상품의 매수·매도를 권하지 않습니다.
            </p>
            <div className="action-row">
              <button className="btn btn-primary" onClick={() => navigate(user ? "/start" : "/login")}>
                {user ? "이번 달 성향 진단하기" : "로그인하고 진단 시작"}
              </button>
              <Link className="btn btn-secondary" to="/glossary">주식 용어 사전</Link>
            </div>
          </div>
          <aside className="card home-flow" aria-label="FinMATE 이용 흐름">
            <span className="eyebrow">한 번의 흐름</span>
            <ol>
              <li><span className="home-step">1</span><div><b>성향 진단</b>5개 문항과 선택형 자유 서술. 등급은 규칙 엔진이 정합니다.</div></li>
              <li><span className="home-step">2</span><div><b>근거 확인</b>당시 답변·확인 플래그·서술과 설문의 차이를 기록으로 남깁니다.</div></li>
              <li><span className="home-step">3</span><div><b>용어·개념 학습</b>등급과 무관하게 모든 개념과 용어를 열어 둡니다.</div></li>
              <li><span className="home-step">4</span><div><b>숫자로 실험</b>금리·보수·레버리지를 바꿔 결과가 어떻게 달라지는지 봅니다.</div></li>
              <li><span className="home-step">5</span><div><b>복습</b>틀린 문항은 1·3·7·14·30일 간격으로 다시 돌아옵니다.</div></li>
            </ol>
          </aside>
        </section>

        <section className="home-section">
          <span className="eyebrow">무엇을 할 수 있나요</span>
          <h2>진단 다음에도 이해는 계속됩니다</h2>
          <p>안정형으로 분류되었다고 위험한 상품을 공부할 수 없는 것은 아닙니다. 거래 가능 여부와 학습 접근을 분리했습니다.</p>
          <div className="home-cards">
            <Link to="/glossary" className="card home-card">
              <h3>주식 용어 사전</h3>
              <p>{termCount ? `${categoryCount}개 분류 ${termCount}개 용어` : "시장 제도부터 세금까지"}를 정의·공식·예시·주의할 점과 함께 정리했습니다. 이름을 몰라도 질문으로 찾을 수 있습니다.</p>
              <span className="more">용어 찾아보기 →</span>
            </Link>
            <Link to="/learn" className="card home-card">
              <h3>개념 트리와 확인 문제</h3>
              <p>자산군 → 상품 구조 → 메커니즘으로 이어지는 개념 지도와, 정답이 서버에만 있는 개념별 확인 문제를 제공합니다.</p>
              <span className="more">개념 학습 →</span>
            </Link>
            <Link to="/practice" className="card home-card">
              <h3>모의투자 실험실</h3>
              <p>실제 종목 없이 가정한 변동성·금리·보수로 포트폴리오를 만들고 충격 시나리오 전후를 비교합니다.</p>
              <span className="more">실험해 보기 →</span>
            </Link>
            <Link to={user ? "/me" : "/login"} className="card home-card">
              <h3>나에게 맞춘 다음 학습</h3>
              <p>진단 결과, 직접 쓴 생각, 틀린 문제를 합쳐 다음에 볼 개념과 용어를 사유와 함께 안내합니다.</p>
              <span className="more">내 학습 보기 →</span>
            </Link>
          </div>
        </section>

        <section className="home-section">
          <span className="eyebrow">학습한 모델, 공개한 숫자</span>
          <h2>모델은 판정하지 않고 보조합니다</h2>
          <p>두 모델 모두 학습에 쓰지 않은 평가 문장으로 측정한 값을 그대로 공개합니다. 성향 등급은 언제나 설문 규칙으로만 정해집니다.</p>
          <div className="home-metrics">
            <div className="home-metric"><strong>{percent(style?.metrics.withinOneGrade)}</strong><span>자유 서술 성향 신호<br />보류 문장 ±1등급 이내</span></div>
            <div className="home-metric"><strong>{percent(style?.metrics.offTopicRejection)}</strong><span>주제와 무관한 문장을<br />판단 보류한 비율</span></div>
            <div className="home-metric"><strong>{percent(retrieval?.metrics.top1)}</strong><span>질문형 용어 검색<br />평가 질문 Top-1</span></div>
            <div className="home-metric"><strong>{percent(retrieval?.metrics.paraphraseTop1)}</strong><span>용어명을 쓰지 않은<br />설명형 질문 Top-1</span></div>
          </div>
          <p style={{ marginTop: 14 }}><Link to="/models">모델 카드와 한계 자세히 보기 →</Link></p>
        </section>

        <section className="home-boundary" aria-label="서비스 범위">
          <div className="card">
            <h3>FinMATE가 하는 일</h3>
            <ul>
              <li>규칙 기반 투자 성향 진단과 판단 근거 기록</li>
              <li>성향과 무관하게 열린 용어·개념 학습과 확인 문제</li>
              <li>가정값 기반 교육용 모의 실험과 계산식·가정 공개</li>
              <li>출처가 있는 교육 콘텐츠와 버전 관리</li>
            </ul>
          </div>
          <div className="card is-not">
            <h3>FinMATE가 하지 않는 일</h3>
            <ul>
              <li>실제 증권계좌 연결, 주문·매매</li>
              <li>특정 종목 추천, 목표주가·수익률 예측</li>
              <li>금융회사의 적합성·적정성 판정 대행</li>
              <li>개인별 세무·법률 자문</li>
            </ul>
          </div>
        </section>
      </main>
    </div>
  );
}
