import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import { useAuth } from "../context/AuthContext";
import { api, ApiError } from "../api/client";
import type { GlossaryCategory, GlossaryQuiz, GlossaryQuizResult, GlossaryTermSummary } from "../api/types";

type Tab = "browse" | "bookmarks" | "quiz";
const levelName: Record<string, string> = { BASIC: "기초", INTERMEDIATE: "중급", ADVANCED: "심화" };

function errorMessage(error: unknown) {
  return error instanceof ApiError ? error.message : "자료를 불러오지 못했습니다. 다시 시도해 주세요.";
}

export default function GlossaryPage() {
  const { isAuthenticated } = useAuth();
  const [tab, setTab] = useState<Tab>("browse");
  const [categories, setCategories] = useState<GlossaryCategory[]>([]);
  const [category, setCategory] = useState("");
  const [level, setLevel] = useState("");
  const [draft, setDraft] = useState("");
  const [query, setQuery] = useState("");
  const [terms, setTerms] = useState<GlossaryTermSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [version, setVersion] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    api.getGlossaryCategories().then(data => {
      setCategories([...data.categories].sort((a, b) => a.order - b.order));
      setVersion(data.contentVersion);
    }).catch(error => setError(errorMessage(error)));
  }, []);

  useEffect(() => {
    if (tab === "quiz") return;
    let current = true;
    setLoading(true);
    setError(null);
    if (tab === "bookmarks" && !isAuthenticated) {
      setTerms([]);
      setTotal(0);
      setLoading(false);
      return;
    }
    const load = tab === "bookmarks"
      ? api.getGlossaryBookmarks().then(data => ({ terms: data.terms, total: data.terms.length, contentVersion: version }))
      : api.getGlossaryTerms({ q: query, category, level });
    load.then(data => {
      if (!current) return;
      setTerms(data.terms);
      setTotal(data.total);
      if (data.contentVersion) setVersion(data.contentVersion);
    }).catch(reason => {
      if (current) setError(errorMessage(reason));
    }).finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [tab, query, category, level, retry, isAuthenticated]);

  return (
    <div className="app-page">
      <Header />
      <main className="glossary-main">
        <div className="glossary-head">
          <span className="eyebrow">STOCK GLOSSARY</span>
          <h1>용어를 이해한 뒤, 시장을 읽습니다.</h1>
          <p className="intro-copy">주식 용어를 짧은 정의에서 실제 예시까지 따라가며 배웁니다. 각 항목의 출처와 주의점도 함께 확인하세요.</p>
          {version && <span className="subtle">콘텐츠 버전 {version}</span>}
        </div>

        <div className="glossary-tabs" role="tablist" aria-label="용어 학습 방식">
          {(["browse", "bookmarks", "quiz"] as const).map(value => (
            <button key={value} type="button" role="tab" aria-selected={tab === value} className={tab === value ? "is-active" : ""} onClick={() => setTab(value)}>
              {{ browse: "용어 탐색", bookmarks: "저장한 용어", quiz: "확인 문제" }[value]}
            </button>
          ))}
        </div>

        {tab === "quiz" ? (
          <GlossaryQuizPanel categories={categories} category={category} setCategory={setCategory} />
        ) : (
          <>
            {tab === "browse" && (
              <form className="glossary-filters card" onSubmit={event => { event.preventDefault(); setQuery(draft.trim()); }}>
                <div className="glossary-search">
                  <label className="field-label" htmlFor="glossary-search">용어 검색</label>
                  <div>
                    <input id="glossary-search" className="field-input" type="search" value={draft} onChange={event => setDraft(event.target.value)} placeholder="용어·영어 이름·설명으로 찾기" />
                    <button className="btn btn-primary" type="submit">찾기</button>
                  </div>
                </div>
                <div className="glossary-filter-row">
                  <label>분야
                    <select className="field-input" value={category} onChange={event => setCategory(event.target.value)}>
                      <option value="">전체 분야</option>
                      {categories.map(item => <option key={item.id} value={item.id}>{item.name} ({item.termCount})</option>)}
                    </select>
                  </label>
                  <label>난이도
                    <select className="field-input" value={level} onChange={event => setLevel(event.target.value)}>
                      <option value="">전체 수준</option>
                      <option value="BASIC">기초</option>
                      <option value="INTERMEDIATE">중급</option>
                      <option value="ADVANCED">심화</option>
                    </select>
                  </label>
                </div>
              </form>
            )}

            <div className="glossary-list-head">
              <h2>{tab === "bookmarks" ? "저장한 용어" : "검색 결과"}</h2>
              {!loading && !error && <span>{total}개</span>}
            </div>
            {tab === "bookmarks" && !isAuthenticated && <div className="card glossary-message"><Link to="/login">로그인</Link>하면 저장한 용어를 볼 수 있습니다.</div>}
            {error && <div className="card glossary-message" role="alert">{error} <button className="btn btn-secondary" onClick={() => setRetry(value => value + 1)}>다시 시도</button></div>}
            {loading && <p role="status" className="subtle">용어를 불러오는 중…</p>}
            {!loading && !error && (tab === "bookmarks" && !isAuthenticated ? null : terms.length === 0 ? (
              <div className="card glossary-message">{tab === "bookmarks" ? "아직 저장한 용어가 없습니다. 탐색에서 용어를 열고 저장해 보세요." : "조건에 맞는 용어가 없습니다. 검색어 또는 필터를 바꿔 보세요."}</div>
            ) : (
              <div className="glossary-list">
                {terms.map(term => (
                  <Link className="card glossary-term-card" key={term.slug} to={`/glossary/${term.slug}`}>
                    <div className="glossary-term-top">
                      <strong>{term.term}</strong>
                      <span className="badge badge-muted">{levelName[term.level] ?? term.level}</span>
                    </div>
                    {term.english && <small>{term.english}</small>}
                    <p>{term.definition}</p>
                    <span className="glossary-term-action">자세히 보기 →</span>
                  </Link>
                ))}
              </div>
            ))}
          </>
        )}
      </main>
    </div>
  );
}

function GlossaryQuizPanel({ categories, category, setCategory }: { categories: GlossaryCategory[]; category: string; setCategory: (value: string) => void }) {
  const [quiz, setQuiz] = useState<GlossaryQuiz | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [result, setResult] = useState<GlossaryQuizResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [grading, setGrading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResult(null);
    setAnswers({});
    try {
      setQuiz(await api.getGlossaryQuiz(category || undefined));
    } catch (reason) {
      setQuiz(null);
      setError(errorMessage(reason));
    } finally {
      setLoading(false);
    }
  }, [category]);

  useEffect(() => { void load(); }, [load]);

  async function grade() {
    if (!quiz || grading) return;
    setGrading(true);
    setError(null);
    try {
      setResult(await api.gradeGlossaryQuiz(quiz, quiz.questions.map(item => ({ questionId: item.id, choiceId: answers[item.id] }))));
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setGrading(false);
    }
  }

  return (
    <section className="glossary-quiz">
      <div className="card glossary-quiz-intro">
        <div><h2>짧게 확인하기</h2><p>정답은 제출 전까지 보이지 않습니다. 틀린 문항은 복습 목록에 남습니다.</p></div>
        <select className="field-input" value={category} onChange={event => setCategory(event.target.value)} aria-label="문제 분야">
          <option value="">전체 분야</option>
          {categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
      </div>
      {error && <p className="error-text" role="alert">{error}</p>}
      {loading && <p className="subtle" role="status">문제를 준비하는 중…</p>}
      {!loading && quiz?.questions.length === 0 && <div className="card glossary-message">이 분야에 준비된 문제가 없습니다.</div>}
      {quiz && quiz.questions.map((item, index) => {
        const feedback = result?.items.find(entry => entry.questionId === item.id);
        return (
          <fieldset className="card glossary-quiz-question" key={item.id} disabled={!!result}>
            <legend>{index + 1}. {item.prompt}</legend>
            {item.choices.map(choice => (
              <label key={choice.id} className={`glossary-choice${feedback?.correctChoiceId === choice.id ? " is-correct" : ""}${feedback && feedback.selectedChoiceId === choice.id && !feedback.correct ? " is-incorrect" : ""}`}>
                <input type="radio" name={item.id} value={choice.id} checked={answers[item.id] === choice.id} onChange={() => setAnswers(prev => ({ ...prev, [item.id]: choice.id }))} />
                <span>{choice.text}</span>
              </label>
            ))}
            {feedback && <p className="glossary-feedback">{feedback.correct ? "정답" : "오답"} · {feedback.explanation}</p>}
          </fieldset>
        );
      })}
      {quiz && quiz.questions.length > 0 && (
        <div className="glossary-quiz-actions">
          {!result ? <button className="btn btn-primary" disabled={grading || Object.keys(answers).length !== quiz.questions.length} onClick={grade}>{grading ? "채점 중…" : "채점하기"}</button> : <><strong>{result.correctCount} / {result.total}개 정답 · {result.scorePercent}점</strong><button className="btn btn-secondary" onClick={() => void load()}>새 문제 풀기</button></>}
          <small>{quiz.notice}</small>
        </div>
      )}
    </section>
  );
}
