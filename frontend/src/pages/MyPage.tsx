import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import { api, ApiError } from "../api/client";
import type { QuizAttemptSummary, ReviewScheduleSummary, WrongAnswerSummary } from "../api/types";

type Tab = "review" | "wrong" | "history";

function date(value: string) {
  return new Date(value).toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric" });
}

export default function MyPage() {
  const [tab, setTab] = useState<Tab>("review");
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
    Promise.all([api.getQuizAttempts(), api.getWrongAnswers(), api.getReviewSchedule()])
      .then(([attemptData, wrongData, scheduleData]) => {
        if (!current) return;
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
        <p className="intro-copy">확인 문제 기록과 복습할 개념을 모았습니다. 틀린 이유를 읽고 용어와 개념으로 돌아가 보세요.</p>
        {error && <div className="card glossary-message" role="alert">{error} <button className="btn btn-secondary" onClick={() => setRetry(value => value + 1)}>다시 시도</button></div>}
        {loading ? <p role="status" className="subtle">학습 기록을 불러오는 중…</p> : !error && <>
          <div className="my-stats">
            <div className="card"><span>확인 문제 풀이</span><strong>{attempts.length}</strong><small>회</small></div>
            <div className="card"><span>다시 볼 문항</span><strong>{wrong.length}</strong><small>개</small></div>
            <div className="card"><span>오늘 복습</span><strong>{dueCount}</strong><small>개념</small></div>
          </div>
          <div className="glossary-tabs" role="tablist" aria-label="내 학습 기록">
            {(["review", "wrong", "history"] as const).map(value => (
              <button type="button" role="tab" key={value} className={tab === value ? "is-active" : ""} aria-selected={tab === value} onClick={() => setTab(value)}>
                {{ review: "복습 일정", wrong: "틀린 문제", history: "풀이 기록" }[value]}
              </button>
            ))}
          </div>
          {tab === "review" && <div className="my-list">
            {schedule.length === 0 ? <Empty title="아직 복습 일정이 없습니다." /> : schedule.map(item => <article key={item.slug} className="card my-item">
              <div><span className={`badge ${item.dueNow ? "badge-warn" : "badge-muted"}`}>{item.dueNow ? "복습할 때" : "예정"}</span><h2>{item.title}</h2><p>{item.incorrectCount}개 오답 · {item.questionCount}개 문항 · 다음 복습 {date(item.dueAt)}</p></div>
              <Link className="btn btn-secondary" to="/learn">개념 보기</Link>
            </article>)}
          </div>}
          {tab === "wrong" && <div className="my-list">
            {wrong.length === 0 ? <Empty title="기록된 오답이 없습니다." /> : wrong.map(item => <article key={`${item.slug}-${item.questionId}`} className="card my-item my-wrong">
              <div><span className="eyebrow">{item.slug} · {date(item.lastAttemptedAt)}</span><h2>{item.prompt}</h2><p>{item.takeaway}</p><div className="my-answer-note"><span>선택한 답: {item.selectedRationale}</span><span>확인할 답: {item.correctRationale}</span></div></div>
              <span className="badge badge-muted">연속 복습 {item.streak}회</span>
            </article>)}
          </div>}
          {tab === "history" && <div className="my-list">
            {attempts.length === 0 ? <Empty title="아직 푼 확인 문제가 없습니다." /> : attempts.map(item => <article key={item.id} className="card my-item">
              <div><span className="eyebrow">{date(item.attemptedAt)} · {item.contentVersion}</span><h2>{item.slug}</h2><p>{item.answered}개 응답 · {item.correctCount} / {item.total}개 정답</p></div>
              <strong>{item.scorePercent}점</strong>
            </article>)}
          </div>}
          <div className="my-next-links"><Link to="/glossary">주식 용어 복습 →</Link><Link to="/learn">개념 학습으로 돌아가기 →</Link></div>
        </>}
      </main>
    </div>
  );
}

function Empty({ title }: { title: string }) {
  return <div className="card glossary-message">{title} <Link to="/learn">개념 학습 시작하기 →</Link></div>;
}
