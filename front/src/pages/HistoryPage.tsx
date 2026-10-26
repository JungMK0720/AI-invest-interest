import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Header from "../components/Header";
import { koreanDate } from "../components/AssessmentAvailability";
import { api } from "../api/client";
import type { HistoryResponse } from "../api/types";

export default function HistoryPage() {
  const [history, setHistory] = useState<HistoryResponse | null>(null);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<string[]>([]);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    setHistory(null); setError(false); setSelected([]);
    api.getHistory(page).then(data => { if (active) setHistory(data); }).catch(() => { if (active) setError(true); });
    return () => { active = false; };
  }, [page, retry]);
  const pair = history?.items.filter(item => selected.includes(item.id)) ?? [];
  const [newer, older] = pair;
  const comparable = pair.length === 2 && newer.evidence && older.evidence
    && newer.evidence.ruleVersion === older.evidence.ruleVersion
    && newer.evidence.questionnaireVersion === older.evidence.questionnaireVersion;
  const changed = comparable ? newer.evidence!.answers.filter(answer => older.evidence!.answers.some(old => old.questionId === answer.questionId && old.answer !== answer.answer)) : [];
  return <div className="app-page"><Header /><main className="journey-main">
    <span className="eyebrow">나의 기록</span><h1>성향의 변화보다,<br />답변의 이유를 먼저.</h1>
    <p className="intro-copy">완료한 진단 두 개를 선택해 비교하세요. 점수가 높아지는 것이 더 좋은 결과를 뜻하지는 않습니다.</p>
    {error && <div role="alert">이력을 불러오지 못했습니다. <button className="btn btn-secondary" onClick={() => setRetry(r => r + 1)}>다시 시도</button></div>}
    {!history && !error && <p role="status">이력을 불러오는 중…</p>}
    {history?.items.length === 0 && <section className="card evidence-card"><h2>아직 완료한 진단이 없어요</h2><Link to="/start">첫 진단 시작하기 →</Link></section>}
    <div className="history-list">{history?.items.map(item => <article className="card history-item" key={item.id}>
      <label><input type="checkbox" checked={selected.includes(item.id)} disabled={selected.length === 2 && !selected.includes(item.id)} onChange={e => setSelected(prev => e.target.checked ? [...prev, item.id] : prev.filter(id => id !== item.id))} /> 비교 선택 <span className="subtle">{koreanDate(item.createdAt)}</span></label>
      <div><strong>{item.profile?.profileLabel}</strong> · {item.profile?.totalScore}점</div><Link to={`/result/${item.id}`}>당시 근거 보기 →</Link>
    </article>)}</div>
    {pair.length === 2 && <section className="card evidence-card" aria-live="polite"><h2>선택한 진단 비교</h2>
      {comparable && newer.profile && older.profile ? <>
        <p>이전 {older.profile.totalScore}점 → 최근 {newer.profile.totalScore}점 · 변화 {newer.profile.totalScore - older.profile.totalScore > 0 ? "+" : ""}{newer.profile.totalScore - older.profile.totalScore}점</p>
        <p>{older.profile.profileLabel} → {newer.profile.profileLabel}</p>
        {changed.length === 0 ? <p>선택한 답변이 같습니다.</p> : <dl>{changed.map(answer => <div key={answer.questionId}><dt>{answer.question}</dt><dd>이전: {older.evidence!.answers.find(a => a.questionId === answer.questionId)?.answer}<br />최근: {answer.answer}</dd></div>)}</dl>}
      </> : <p>질문 또는 판정 기준이 다르거나 당시 기록이 없어 점수 차이를 직접 비교하지 않습니다. 각 결과의 근거를 확인해 주세요.</p>}
    </section>}
    {history && <div className="action-row"><button className="btn btn-secondary" disabled={page === 0} onClick={() => setPage(p => p - 1)}>이전 기록 페이지</button><span>{page + 1}페이지</span><button className="btn btn-secondary" disabled={!history.hasNext} onClick={() => setPage(p => p + 1)}>다음 기록 페이지</button></div>}
    <Link to="/learn">다음 진단 전, 상품 개념 더 배우기 →</Link>
  </main></div>;
}
