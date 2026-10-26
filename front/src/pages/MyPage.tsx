import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import { api, ApiError } from "../api/client";
import type { LearningPathResponse, LearningPathStep, QuizAttemptSummary, ReviewScheduleSummary, WrongAnswerSummary } from "../api/types";

type Tab = "review" | "wrong" | "history";

const REASON_LABEL: Record<string, string> = {
  DUE_REVIEW: "복습",
  NARRATIVE_MISMATCH: "서술·설문 차이",
  PROFILE: "현재 성향",
  STARTER: "처음 시작",
  SHORT_HORIZON: "짧은 투자 기간",
  HIGH_CONCENTRATION: "높은 투자 비중",
  CONFLICTING_PREFERENCES: "서로 다른 답변",
  PRINCIPAL_FOCUS: "원금 보전 중시",
  SHORT_TERM_NEED: "단기 자금",
  LONG_TERM: "장기 투자",
  HIGH_RETURN: "고수익 추구",
  LOSS_EXPERIENCE: "손실 경험",
  BEGINNER: "경험 부족",
  LEVERAGE_INTEREST: "레버리지 관심",
  INCOME_FOCUS: "현금흐름 선호",
  FOMO_HERDING: "쏠림 신호",
};

function date(value: string) {
  return new Date(value).toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric" });
}

/** 개념이면 개념 학습 화면, 용어면 용어 상세로 보낸다. 같은 slug가 둘 다 있을 수 있어 target을 따른다. */
function linkFor(target: "CONCEPT" | "TERM", slug: string) {
  return target === "TERM" ? `/glossary/${encodeURIComponent(slug)}` : `/learn?concept=${encodeURIComponent(slug)}`;
}

export default function MyPage() {
  const [tab, setTab] = useState<Tab>("review");
  const [path, setPath] = useState<LearningPathResponse | null>(null);
  const [attempts, setAttempts] = useState<QuizAttemptSummary[]>([]);
  const [wrong, setWrong] = useState<WrongAnswerSummary[]>([]);
  const [schedule, setSchedule] = useState<ReviewScheduleSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    Promise.all([api.getLearningPath(), api.getQuizAttempts(), api.getWrongAnswers(), api.getReviewSchedule()])
      .then(([pathData, attemptData, wrongData, scheduleData]) => {
        if (!current) return;
        setPath(pathData);
        setAttempts(attemptData.items);
        setWrong(wrongData.items);
        setSchedule(scheduleData.items);
      })
      .catch(reason => {
        if (current) setError(reason instanceof ApiError ? reason.message : "학습 기록을 불러오지 못했습니다.");
      })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [retry]);

  const dueCount = schedule.filter(item => item.dueNow).length;

  return (
    <div className="app-page">
      <Header />
      <main className="my-main">
        <span className="eyebrow">MY LEARNING</span>
        <h1>한 번 맞히는 것보다, 다시 설명할 수 있게.</h1>
        <p className="intro-copy">진단 결과와 직접 쓴 생각, 틀린 문제를 합쳐 다음에 볼 것을 정리했습니다. 모든 항목에 왜 추천했는지 사유가 붙어 있습니다.</p>
        {error && <div className="card glossary-message" role="alert">{error} <button className="btn btn-secondary" onClick={() => setRetry(value => value + 1)}>다시 시도</button></div>}
        {loading ? <p role="status" className="subtle">학습 기록을 불러오는 중…</p> : !error && <>
          {path && <LearningPath path={path} />}

          <div className="my-stats">
            <div className="card"><span>확인 문제 풀이</span><strong>{attempts.length}</strong><small>회</small></div>
            <div className="card"><span>다시 볼 문항</span><strong>{wrong.length}</strong><small>개</small></div>
            <div className="card"><span>오늘 복습</span><strong>{dueCount}</strong><small>항목</small></div>
          </div>
          <div className="glossary-tabs" role="tablist" aria-label="내 학습 기록">
            {(["review", "wrong", "history"] as const).map(value => (
              <button type="button" role="tab" key={value} className={tab === value ? "is-active" : ""} aria-selected={tab === value} onClick={() => setTab(value)}>
                {{ review: "복습 일정", wrong: "틀린 문제", history: "풀이 기록" }[value]}
              </button>
            ))}
          </div>
          {tab === "review" && <div className="my-list">
            {schedule.length === 0 ? <Empty title="아직 복습 일정이 없습니다." /> : schedule.map(item => <article key={`${item.target}-${item.slug}`} className="card my-item">
              <div><span className={`badge ${item.dueNow ? "badge-warn" : "badge-muted"}`}>{item.dueNow ? "복습할 때" : "예정"}</span> <span className="badge badge-muted">{item.target === "TERM" ? "용어" : "개념"}</span><h2>{item.title}</h2><p>{item.incorrectCount}개 오답 · {item.questionCount}개 문항 · 다음 복습 {date(item.dueAt)}</p></div>
              <Link className="btn btn-secondary" to={linkFor(item.target, item.slug)}>{item.target === "TERM" ? "용어 보기" : "개념 보기"}</Link>
            </article>)}
          </div>}
          {tab === "wrong" && <div className="my-list">
            {wrong.length === 0 ? <Empty title="기록된 오답이 없습니다." /> : wrong.map(item => <article key={`${item.slug}-${item.questionId}`} className="card my-item my-wrong">
              <div><span className="eyebrow">{item.title} · {date(item.lastAttemptedAt)}</span><h2>{item.prompt}</h2><p>{item.takeaway}</p><div className="my-answer-note"><span>선택한 답: {item.selectedRationale}</span><span>확인할 답: {item.correctRationale}</span></div></div>
              <div style={{ display: "flex", flexDirection: "column", gap: 8, alignItems: "flex-end" }}>
                <span className="badge badge-muted">연속 정답 {item.streak}회</span>
                <Link className="btn btn-secondary btn-small" to={linkFor(item.target, item.slug)}>다시 공부하기</Link>
              </div>
            </article>)}
          </div>}
          {tab === "history" && <div className="my-list">
            {attempts.length === 0 ? <Empty title="아직 푼 확인 문제가 없습니다." /> : attempts.map(item => <article key={item.id} className="card my-item">
              <div><span className="eyebrow">{date(item.attemptedAt)}</span><h2>{item.slug === "glossary" ? "주식 용어 확인 문제" : item.slug}</h2><p>{item.answered}개 응답 · {item.correctCount} / {item.total}개 정답</p></div>
              <strong>{item.scorePercent}점</strong>
            </article>)}
          </div>}
          <div className="my-next-links"><Link to="/glossary">주식 용어 복습 →</Link><Link to="/learn">개념 학습으로 돌아가기 →</Link></div>
        </>}
      </main>
    </div>
  );
}

function LearningPath({ path }: { path: LearningPathResponse }) {
  const basis = path.basis;
  return (
    <section className="home-section" aria-labelledby="path-title">
      <span className="eyebrow">나에게 맞춘 다음 학습</span>
      <h2 id="path-title">지금 보면 좋은 것</h2>
      <div className="card path-basis">
        {basis ? <>
          <span>최근 진단 <strong>{basis.profileLabel ?? "결과 없음"}</strong>{basis.assessedAt && ` · ${date(basis.assessedAt)}`}</span>
          {basis.narrativeTags.length > 0 && <span className="tag-row">{basis.narrativeTags.map(tag => <span key={tag.code} className="tag-chip">{tag.label}</span>)}</span>}
          {basis.narrativeMismatch && <span className="badge badge-warn">서술과 설문이 다른 방향</span>}
          <Link to={`/result/${basis.assessmentId}`}>진단 결과 보기 →</Link>
        </> : <>
          <span>아직 진단 기록이 없어 처음 배우는 분을 위한 순서로 안내합니다.</span>
          <Link to="/start">성향 진단하기 →</Link>
        </>}
      </div>
      <div className="path-grid" style={{ marginTop: 14 }}>
        <PathColumn title="복습할 때가 된 항목" steps={path.reviews} empty="지금 복습할 항목이 없습니다." />
        <PathColumn title="다음 학습 개념" steps={path.concepts} empty="추천할 개념이 없습니다. 모두 확인했어요!" />
        <PathColumn title="함께 볼 용어" steps={path.terms} empty="추천할 용어가 없습니다." />
      </div>
      <p className="subtle" style={{ marginTop: 10 }}>{path.notice} · 규칙 {path.rulesVersion}</p>
    </section>
  );
}

function PathColumn({ title, steps, empty }: { title: string; steps: LearningPathStep[]; empty: string }) {
  return (
    <div className="card path-column">
      <h3>{title}</h3>
      {steps.length === 0 ? <p className="subtle" style={{ margin: 0 }}>{empty}</p> : steps.map(step => (
        <Link key={`${step.kind}-${step.target}-${step.slug}`} className="path-step" to={linkFor(step.target, step.slug)}>
          <em>{REASON_LABEL[step.reasonCode] ?? "관심사"}</em>
          <strong>{step.title}</strong>
          <span>{step.reason}</span>
        </Link>
      ))}
    </div>
  );
}

function Empty({ title }: { title: string }) {
  return <div className="card glossary-message">{title} <Link to="/learn">개념 학습 시작하기 →</Link></div>;
}
