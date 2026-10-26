import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Header from "../components/Header";
import Stepper from "../components/Stepper";
import AssessmentAvailability from "../components/AssessmentAvailability";
import { api } from "../api/client";
import type { AvailabilityResponse } from "../api/types";

export default function StartPage() {
  const navigate = useNavigate();
  const [availability, setAvailability] = useState<AvailabilityResponse | null>(null);
  const [error, setError] = useState(false);
  function load() {
    setError(false);
    api.getAvailability().then(setAvailability).catch(() => setError(true));
  }
  useEffect(load, []);
  return <div className="app-page"><Header /><Stepper activeIndex={0} />
    <main className="journey-main">
      <span className="eyebrow">나를 이해하고, 상품을 이해하는 투자 준비</span>
      <h1>진단 다음에도,<br />이해는 계속될 수 있도록.</h1>
      <p className="intro-copy">5개 질문으로 투자 성향을 확인하고, 답변의 근거와 상품의 구조를 함께 살펴보세요. 성향 등급은 규칙으로 계산하고 AI가 설명을 돕습니다.</p>
      {error ? <div role="alert">진단 가능 일정을 불러오지 못했습니다. <button className="btn btn-secondary" onClick={load}>다시 시도</button></div>
        : availability ? <AssessmentAvailability value={availability} /> : <p role="status">진단 가능 일정을 확인하고 있어요.</p>}
      <div className="action-row">
        <button className="btn btn-primary" disabled={!availability?.canSubmit || error} onClick={() => navigate("/diagnosis")}>이번 달 진단 시작</button>
        <Link className="btn btn-secondary" to="/history">내 진단 이력</Link>
        <Link className="btn btn-secondary" to="/learn">상품 기초 학습</Link>
      </div>
      <div className="feature-grid">
        <div className="card"><h2>왜 이런 성향일까?</h2><p>진단 당시의 답변과 확인할 사항을 결과에 남깁니다.</p></div>
        <div className="card"><h2>왜 안내되지 않을까?</h2><p>성향과 상품 위험의 차이를 확인하고 기초 개념부터 배웁니다.</p></div>
        <div className="card"><h2>무엇이 달라졌을까?</h2><p>다음 달에는 이전 결과와 달라진 답변을 비교합니다.</p></div>
      </div>
      <p className="subtle">교육용 탐색 서비스입니다. 실제 상품 가입·매매 기능은 제공하지 않습니다.</p>
    </main>
  </div>;
}
