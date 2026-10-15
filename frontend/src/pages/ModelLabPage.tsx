import { useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import { api, ApiError } from "../api/client";
import type { StyleCheckResponse, TermSearchResponse } from "../api/types";

function message(error: unknown): string {
  return error instanceof ApiError ? error.message : "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
}

export default function ModelLabPage() {
  const [styleText, setStyleText] = useState("");
  const [grade, setGrade] = useState(3);
  const [styleResult, setStyleResult] = useState<StyleCheckResponse | null>(null);
  const [styleError, setStyleError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);
  const [question, setQuestion] = useState("");
  const [terms, setTerms] = useState<TermSearchResponse | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [searching, setSearching] = useState(false);

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

  return (
    <div className="app-page">
      <Header />
      <main className="model-main">
        <div className="model-hero">
          <span className="eyebrow">FINMATE MODEL LAB</span>
          <h1>모델이 보조할 수 있는 두 가지</h1>
          <p className="intro-copy">
            직접 쓴 투자 생각에서 설문 결과와 다른 신호를 찾아보고, 질문과 관련된 용어를 찾습니다.
            모델 결과는 학습용 참고 정보이며 설문 등급이나 상품 안내 범위를 바꾸지 않습니다.
          </p>
        </div>

        <div className="model-grid">
          <section className="card model-panel" aria-labelledby="style-title">
            <div className="model-panel-head">
              <span className="model-index">01</span>
              <div>
                <h2 id="style-title">내 말과 설문 결과 비교</h2>
                <p>투자에 대한 생각을 자유롭게 적으면 불일치 신호만 확인합니다.</p>
              </div>
            </div>
            <form onSubmit={checkStyle} className="model-form">
              <label className="field-label" htmlFor="questionnaire-grade">설문에서 받은 등급</label>
              <select id="questionnaire-grade" className="field-input" value={grade} onChange={event => setGrade(Number(event.target.value))}>
                {[1, 2, 3, 4, 5].map(value => <option key={value} value={value}>{value}등급</option>)}
              </select>
              <label className="field-label" htmlFor="investment-thoughts">투자에 대한 생각</label>
              <textarea
                id="investment-thoughts"
                className="field-input model-textarea"
                value={styleText}
                onChange={event => setStyleText(event.target.value)}
                placeholder="예: 원금 손실이 걱정되지만 오래 투자할 수 있고, 단기 가격 변화에는 흔들리지 않으려고 합니다."
                minLength={12}
                maxLength={1000}
                required
              />
              <button type="submit" className="btn btn-primary" disabled={checking || styleText.trim().length < 12}>
                {checking ? "분석 중…" : "비교해 보기"}
              </button>
            </form>
            {styleError && <p className="error-text" role="alert">{styleError}</p>}
            {styleResult && (
              <div className="model-result" role="status">
                <span className="model-result-label">분석 결과 · {styleResult.modelVersion}</span>
                {styleResult.status === "INSUFFICIENT" ? (
                  <p>판단하기에 표현이 부족합니다. 위험을 받아들이는 정도와 투자 기간을 조금 더 적어 주세요.</p>
                ) : (
                  <>
                    <strong>{styleResult.estimatedProfileLabel ?? "추정 불가"} · {styleResult.estimatedGrade ?? "–"}등급</strong>
                    {styleResult.confidence !== null && (
                      <span>모델 확신도 {Math.round(styleResult.confidence <= 1 ? styleResult.confidence * 100 : styleResult.confidence)}%</span>
                    )}
                    <p>{styleResult.mismatch ? "설문 결과와 차이가 있어 답변 근거를 다시 살펴볼 수 있습니다." : "설문 결과와 큰 차이가 감지되지 않았습니다."}</p>
                  </>
                )}
                <small>{styleResult.notice}</small>
              </div>
            )}
          </section>

          <section className="card model-panel" aria-labelledby="term-title">
            <div className="model-panel-head">
              <span className="model-index">02</span>
              <div>
                <h2 id="term-title">질문으로 용어 찾기</h2>
                <p>모르는 표현을 그대로 질문하면 관련 용어를 찾아줍니다.</p>
              </div>
            </div>
            <form onSubmit={search} className="model-form">
              <label className="field-label" htmlFor="term-question">궁금한 점</label>
              <textarea
                id="term-question"
                className="field-input model-textarea"
                value={question}
                onChange={event => setQuestion(event.target.value)}
                placeholder="예: 주식 가격이 급하게 떨어질 때 주문이 어느 가격에 체결되는지 궁금해요."
                minLength={3}
                maxLength={500}
                required
              />
              <button type="submit" className="btn btn-primary" disabled={searching || question.trim().length < 3}>
                {searching ? "찾는 중…" : "관련 용어 찾기"}
              </button>
            </form>
            {searchError && <p className="error-text" role="alert">{searchError}</p>}
            {terms && (
              <div className="model-result" role="status">
                <span className="model-result-label">검색 결과 · {terms.modelVersion}</span>
                {terms.results.length === 0 ? (
                  <p>관련 용어를 찾지 못했습니다. 다른 표현으로 질문해 보세요.</p>
                ) : (
                  <ol className="model-term-results">
                    {terms.results.map(result => (
                      <li key={result.slug}>
                        <Link to={`/glossary/${result.slug}`}>{result.term}</Link>
                        <span>관련도 {Math.round(result.score <= 1 ? result.score * 100 : result.score)}%</span>
                      </li>
                    ))}
                  </ol>
                )}
              </div>
            )}
          </section>
        </div>

        <p className="model-footnote">이 화면은 투자 판단이나 수익률 예측을 제공하지 않습니다. 용어 정의와 설문 결과는 각각 출처와 계산 근거를 확인하세요.</p>
      </main>
    </div>
  );
}
