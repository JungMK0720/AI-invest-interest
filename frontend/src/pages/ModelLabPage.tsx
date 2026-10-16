import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import { api, ApiError } from "../api/client";
import type { ModelCard, ModelsResponse, StyleCheckResponse, TermSearchResponse } from "../api/types";

const PROFILE_LABELS: Record<string, string> = {
  AGGRESSIVE: "공격투자형",
  ACTIVE: "적극투자형",
  NEUTRAL: "위험중립형",
  CONSERVATIVE: "안정추구형",
  STABLE: "안정형",
};

const METRIC_LABELS: Record<string, string> = {
  accuracy: "정확도",
  macroF1: "macro F1",
  withinOneGrade: "±1등급 이내",
  coverage: "판단 비율",
  accuracyWhenEmitted: "판단 시 정확도",
  tagMicroF1: "태그 micro F1",
  expectedCalibrationError: "보정 오차(ECE)",
  offTopicRejection: "주제 무관 보류율",
  top1: "Top-1",
  top5: "Top-5",
  mrr: "MRR",
  paraphraseTop1: "설명형 Top-1",
  paraphraseTop5: "설명형 Top-5",
};

const BASELINE_LABELS: Record<string, string> = {
  majorityClass: "최빈 클래스",
  keywordRules: "키워드 규칙",
  v1NaiveBayes40: "v1 나이브 베이즈",
  v1ConcatenatedTop1: "v1 검색 Top-1",
  v1ConcatenatedParaphraseTop1: "v1 설명형 Top-1",
};

function message(error: unknown): string {
  return error instanceof ApiError ? error.message : "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}

function metric(key: string, value: number) {
  return key === "expectedCalibrationError" ? value.toFixed(3) : `${Math.round(value * 1000) / 10}%`;
}

/**
 * 모델 실험실. 두 보조 모델을 직접 써 보고, 학습 데이터·평가 지표·한계를 모델 카드로 확인한다.
 * 화면의 숫자는 모두 /ml/models가 돌려주는 model-card.json에서 온다.
 */
export default function ModelLabPage() {
  const [models, setModels] = useState<ModelsResponse | null>(null);
  const [modelsError, setModelsError] = useState(false);
  const [styleText, setStyleText] = useState("");
  const [grade, setGrade] = useState(3);
  const [styleResult, setStyleResult] = useState<StyleCheckResponse | null>(null);
  const [styleError, setStyleError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);
  const [question, setQuestion] = useState("");
  const [terms, setTerms] = useState<TermSearchResponse | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    let alive = true;
    api.getModels().then(data => { if (alive) setModels(data); }).catch(() => { if (alive) setModelsError(true); });
    return () => { alive = false; };
  }, []);

  async function checkStyle(event: React.FormEvent) {
    event.preventDefault();
    setChecking(true);
    setStyleError(null);
    setStyleResult(null);
    try {
      setStyleResult(await api.checkStyle(styleText.trim(), grade));
    } catch (error) {
      setStyleError(message(error));
    } finally {
      setChecking(false);
    }
  }

  async function search(event: React.FormEvent) {
    event.preventDefault();
    setSearching(true);
    setSearchError(null);
    setTerms(null);
    try {
      setTerms(await api.searchTermsByQuestion(question.trim()));
    } catch (error) {
      setSearchError(message(error));
    } finally {
      setSearching(false);
    }
  }

  const topLabel = styleResult && Object.keys(styleResult.probabilities).length > 0
    ? Object.entries(styleResult.probabilities).sort((a, b) => b[1] - a[1])[0][0]
    : null;

  return (
    <div className="app-page">
      <Header />
      <main className="model-main">
        <div className="model-hero">
          <span className="eyebrow">FINMATE MODEL LAB</span>
          <h1>학습한 모델을 직접 써 보고, 한계까지 확인하세요</h1>
          <p className="intro-copy">
            FinMATE의 두 모델은 Python으로 학습해 서버에 내장했습니다. 직접 쓴 투자 생각에서 설문과 다른 신호와 관심사를 읽고,
            용어 이름을 몰라도 질문으로 용어를 찾습니다. 모델은 성향 등급이나 상품 안내 범위를 바꾸지 않습니다.
          </p>
        </div>

        <div className="model-grid">
          <section className="card model-panel" aria-labelledby="style-title">
            <div className="model-panel-head">
              <span className="model-index">01</span>
              <div>
                <h2 id="style-title">내 말과 설문 결과 비교</h2>
                <p>투자에 대한 생각을 자유롭게 적으면 성향 신호와 관심사를 읽고 다음 학습을 안내합니다.</p>
              </div>
            </div>
            <form onSubmit={checkStyle} className="model-form">
              <label className="field-label" htmlFor="questionnaire-grade">설문에서 받은 등급</label>
              <select id="questionnaire-grade" className="field-input" value={grade} onChange={event => setGrade(Number(event.target.value))}>
                {Object.values(PROFILE_LABELS).map((label, index) => <option key={label} value={index + 1}>{index + 1}등급 · {label}</option>)}
              </select>
              <label className="field-label" htmlFor="investment-thoughts">투자에 대한 생각</label>
              <textarea
                id="investment-thoughts"
                className="field-input model-textarea"
                value={styleText}
                onChange={event => setStyleText(event.target.value)}
                placeholder="예: 예전에 주식으로 크게 잃어서 겁이 나요. 그래도 10년 이상 노후 자금으로 묻어 둘 생각이에요."
                minLength={8}
                maxLength={1000}
                required
              />
              <button type="submit" className="btn btn-primary" disabled={checking || styleText.trim().length < 8}>
                {checking ? "분석 중…" : "비교해 보기"}
              </button>
            </form>
            {styleError && <p className="error-text" role="alert">{styleError}</p>}
            {styleResult && (
              <div className="model-result" role="status">
                <span className="model-result-label">분석 결과 · 모델 {styleResult.modelVersion}</span>
                {styleResult.status === "INSUFFICIENT" ? (
                  <p>판단을 보류했습니다. 위험을 받아들이는 정도나 돈이 필요한 시점을 조금 더 적어 주세요.</p>
                ) : (
                  <>
                    <strong>{styleResult.estimatedProfileLabel} · {styleResult.estimatedGrade}등급 (확신도 {Math.round((styleResult.confidence ?? 0) * 100)}%)</strong>
                    <p>{styleResult.mismatch ? "설문 등급과 2단계 이상 차이가 있습니다. 실제 진단이었다면 답변을 다시 확인하라는 표시가 붙습니다." : "설문 등급과 큰 차이가 감지되지 않았습니다."}</p>
                  </>
                )}
                {Object.keys(styleResult.probabilities).length > 0 && (
                  <div className="prob-bars" aria-label="성향별 확률">
                    {Object.entries(styleResult.probabilities).map(([label, value]) => (
                      <div key={label} className={`prob-bar${label === topLabel ? " is-top" : ""}`}>
                        <span>{PROFILE_LABELS[label] ?? label}</span>
                        <div className="bar-track"><i style={{ width: `${Math.round(value * 100)}%` }} /></div>
                        <span>{Math.round(value * 100)}%</span>
                      </div>
                    ))}
                  </div>
                )}
                {styleResult.tags.length > 0 && (
                  <div className="tag-row" aria-label="관심사 태그">
                    {styleResult.tags.map(tag => <span key={tag.code} className="tag-chip">{tag.label} <small>{Math.round(tag.score * 100)}%</small></span>)}
                  </div>
                )}
                {styleResult.learningLinks.map(link => (
                  <div key={link.code} className="model-learning-link">
                    <p>{link.message}</p>
                    <div className="tag-row">
                      {link.concepts.slice(0, 2).map(item => <Link key={item.slug} className="btn btn-secondary btn-small" to={`/learn?concept=${item.slug}`}>개념 · {item.title}</Link>)}
                      {link.terms.slice(0, 2).map(item => <Link key={item.slug} className="btn btn-secondary btn-small" to={`/glossary/${item.slug}`}>용어 · {item.title}</Link>)}
                    </div>
                  </div>
                ))}
                <small>{styleResult.notice}</small>
              </div>
            )}
          </section>

          <section className="card model-panel" aria-labelledby="term-title">
            <div className="model-panel-head">
              <span className="model-index">02</span>
              <div>
                <h2 id="term-title">질문으로 용어 찾기</h2>
                <p>모르는 표현을 문장 그대로 물어보면 관련 용어를 찾습니다.</p>
              </div>
            </div>
            <form onSubmit={search} className="model-form">
              <label className="field-label" htmlFor="term-question">궁금한 점</label>
              <textarea
                id="term-question"
                className="field-input model-textarea"
                value={question}
                onChange={event => setQuestion(event.target.value)}
                placeholder="예: 빌린 돈을 못 갚으면 증권사가 내 주식을 마음대로 팔아 버리는 것"
                minLength={2}
                maxLength={500}
                required
              />
              <button type="submit" className="btn btn-primary" disabled={searching || question.trim().length < 2}>
                {searching ? "찾는 중…" : "관련 용어 찾기"}
              </button>
            </form>
            {searchError && <p className="error-text" role="alert">{searchError}</p>}
            {terms && (
              <div className="model-result" role="status">
                <span className="model-result-label">검색 결과 · 모델 {terms.modelVersion} · 공개 용어 {terms.indexedTerms}개</span>
                {terms.results.length === 0 ? (
                  <p>관련 용어를 찾지 못했습니다. 다른 표현으로 질문해 보세요.</p>
                ) : terms.results.map((hit, index) => (
                  <Link key={hit.slug} to={`/glossary/${hit.slug}`} className="search-hit">
                    <header><strong>{index + 1}. {hit.term}</strong><span className="subtle">점수 {hit.score.toFixed(2)}{hit.nameMatched ? " · 이름 일치" : ""}</span></header>
                    <p>{hit.definition}</p>
                  </Link>
                ))}
              </div>
            )}
          </section>
        </div>

        <section className="home-section" aria-labelledby="card-title" style={{ marginTop: 36 }}>
          <span className="eyebrow">MODEL CARDS</span>
          <h2 id="card-title">무엇으로 학습했고, 얼마나 맞히고, 어디서 틀리는가</h2>
          <p>모든 지표는 학습에 쓰지 않은 평가 데이터로 측정했습니다. 재현 방법과 전체 보고서는 저장소의 ml/README.md와 ml/reports/evaluation.json에 있습니다.</p>
          {modelsError && <div className="card glossary-message" role="alert">모델 카드를 불러오지 못했습니다.</div>}
          {models && <div className="model-cards">{models.card.models.map(card => <ModelCardView key={card.id} card={card} />)}</div>}
        </section>

        <p className="model-footnote">이 화면은 투자 판단이나 수익률 예측을 제공하지 않습니다. 용어 정의와 설문 결과는 각각 출처와 계산 근거를 확인하세요.</p>
      </main>
    </div>
  );
}

function ModelCardView({ card }: { card: ModelCard }) {
  return (
    <article className="card model-card">
      <span className="eyebrow">{card.id} · v{card.version}</span>
      <h2>{card.name}</h2>
      <p className="subtle" style={{ margin: 0 }}>{card.task}</p>
      <div className="metric-chips">
        {Object.entries(card.metrics).map(([key, value]) => (
          <div key={key} className="metric-chip"><strong>{metric(key, value)}</strong><span>{METRIC_LABELS[key] ?? key}</span></div>
        ))}
      </div>
      <dl>
        <dt>알고리즘</dt><dd>{card.algorithm}</dd>
        <dt>학습 데이터</dt><dd>{card.trainingData}</dd>
        <dt>평가 데이터</dt><dd>{card.evaluationData}</dd>
        <dt>비교 기준</dt><dd>{Object.entries(card.baselines).map(([key, value]) => `${BASELINE_LABELS[key] ?? key} ${metric(key, value)}`).join(" · ")}</dd>
        <dt>쓰는 곳</dt><dd>{card.intendedUse}</dd>
        <dt>쓰지 않는 곳</dt><dd>{card.notIntendedUse}</dd>
      </dl>
      <details>
        <summary>알려진 한계 {card.limitations.length}가지</summary>
        <ul>{card.limitations.map(item => <li key={item}>{item}</li>)}</ul>
      </details>
    </article>
  );
}
