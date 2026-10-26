import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import Header from "../components/Header";
import Stepper from "../components/Stepper";
import { api } from "../api/client";
import type { AssessmentResultResponse, NarrativeSignal, StyleCheckResponse } from "../api/types";

const GRADE_LABELS = ["공격투자형", "적극투자형", "위험중립형", "안정추구형", "안정형"];
const GRADE_COLORS = ["#E5484D", "#ED7D4C", "#F0B429", "#8FBF3F", "#3FAE64"];

export default function ResultPage() {
  const { assessmentId } = useParams<{ assessmentId: string }>();
  const navigate = useNavigate();
  const [data, setData] = useState<AssessmentResultResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [styleText, setStyleText] = useState("");
  const [styleResult, setStyleResult] = useState<StyleCheckResponse | null>(null);
  const [styleError, setStyleError] = useState<string | null>(null);
  const [checkingStyle, setCheckingStyle] = useState(false);

  useEffect(() => {
    if (!assessmentId) return;
    api
      .getAssessment(assessmentId)
      .then(setData)
      .catch(() => setError("진단 결과를 불러오지 못했습니다."));
  }, [assessmentId]);

  if (error) {
    return (
      <Shell>
        <div style={{ textAlign: "center", color: "var(--danger)", fontSize: 14 }}>{error}</div>
      </Shell>
    );
  }

  if (data && !data.profile) return <Shell><section className="policy-card"><h2>{data.status === "FAILED" ? "진단을 완료하지 못했습니다" : "진단을 처리하고 있습니다"}</h2><p>시작 화면에서 진단 가능 상태를 확인해 주세요. 처리가 오래 지속되면 운영 확인이 필요합니다.</p><button className="btn btn-secondary" onClick={() => navigate("/start")}>시작 화면으로</button></section></Shell>;

  if (!data || !data.profile) {
    return (
      <Shell>
        <div style={{ textAlign: "center", color: "var(--text-muted)", fontSize: 14 }}>불러오는 중...</div>
      </Shell>
    );
  }

  const { profile } = data;
  const isFallback = profile.reasoningSource === "TEMPLATE";

  async function checkWrittenStyle(event: React.FormEvent) {
    event.preventDefault();
    if (!data?.profile) return;
    setCheckingStyle(true);
    setStyleError(null);
    setStyleResult(null);
    try {
      setStyleResult(await api.checkStyle(styleText.trim(), data.profile.profileGrade));
    } catch {
      setStyleError("문장 분석을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setCheckingStyle(false);
    }
  }

  return (
    <Shell>
      <div style={{ width: 640, maxWidth: "100%", display: "flex", flexDirection: "column", gap: 16 }}>
        <div style={{ fontWeight: 700, fontSize: 19 }}>회원님의 투자 성향 분석 결과입니다</div>

        <div className="card" style={{ padding: "26px 28px", display: "flex", flexDirection: "column", gap: 18 }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <span className="badge badge-primary" style={{ fontSize: 14, padding: "7px 14px" }}>
                {profile.profileLabel}
              </span>
              <span style={{ fontSize: 12.5, color: "var(--text-faint)" }}>5단계 중 {profile.profileGrade}등급</span>
            </div>
            <div style={{ fontSize: 13, color: "var(--text-muted)" }}>
              성향 점수 <strong style={{ color: "var(--text)" }}>{profile.totalScore}</strong>/100
            </div>
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
            <div style={{ display: "flex", width: "100%", height: 9, borderRadius: 6, overflow: "hidden", gap: 2 }}>
              {GRADE_COLORS.map((color, i) => (
                <div
                  key={color}
                  style={{
                    flex: 1,
                    background: color,
                    opacity: i + 1 === profile.profileGrade ? 1 : 0.28,
                  }}
                />
              ))}
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: "var(--text-faint)" }}>
              {GRADE_LABELS.map((label, i) => (
                <span key={label} style={i + 1 === profile.profileGrade ? { color: "var(--primary)", fontWeight: 700 } : undefined}>
                  {label}
                </span>
              ))}
            </div>
          </div>
        </div>

        {isFallback && (
          <div
            style={{
              background: "var(--warn-bg)",
              border: "1.5px solid var(--warn-border)",
              borderRadius: 12,
              padding: "13px 16px",
              display: "flex",
              gap: 10,
              alignItems: "center",
            }}
          >
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
              <circle cx="12" cy="12" r="9" stroke="#8A5A00" strokeWidth="1.8" />
              <line x1="12" y1="8" x2="12" y2="13" stroke="#8A5A00" strokeWidth="1.8" strokeLinecap="round" />
              <circle cx="12" cy="16" r="1" fill="#8A5A00" />
            </svg>
            <span style={{ fontSize: 12.5, color: "var(--warn-text)" }}>
              AI 설명을 사용할 수 없어 기본 설명으로 안내합니다. 성향 등급과 탐색 카테고리는 정상적으로 산출되었습니다.
            </span>
          </div>
        )}

        <div className="card" style={{ padding: "24px 28px", display: "flex", flexDirection: "column", gap: 12 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span className={isFallback ? "badge badge-muted" : "badge badge-primary"}>
              {isFallback ? "기본 설명" : "AI 생성"}
            </span>
            <span style={{ fontSize: 13, fontWeight: 700 }}>분석 근거</span>
          </div>
          <div style={{ fontSize: 13.5, color: "var(--text-muted)", lineHeight: 1.7 }}>{profile.reasoning}</div>
        </div>

        {data.evidence ? <section className="card evidence-card">
          <h2>진단 당시의 답변</h2>
          <p className="subtle">결과를 이해하기 위한 기록입니다. 지금 다시 답해도 이번 달 결과는 변경되지 않습니다.</p>
          <dl>{data.evidence.answers.map(answer => <div key={answer.questionId}><dt>{answer.question}</dt><dd>{answer.answer}</dd></div>)}</dl>
          {data.evidence.reviewFlags.length > 0 && <div className="policy-card"><strong>상품을 살펴보기 전에 확인해 주세요</strong>{data.evidence.reviewFlags.map(flag => <p key={flag.code}>{flag.message}</p>)}</div>}
        </section> : <p className="subtle">이전 버전의 진단은 답변 근거 기록을 제공하지 않습니다.</p>}
        {data.evidence?.narrative ? <NarrativeSignalCard signal={data.evidence.narrative} /> : <section className="card result-style-card">
          <span className="eyebrow">선택 · 자유 서술</span>
          <h2>내가 쓴 말도 결과와 비교해 볼까요?</h2>
          <p className="subtle">진단할 때 서술을 건너뛰었습니다. 지금 적어 보면 모델이 설문과 다른 신호가 있는지 표시합니다. 이 비교는 저장되지 않고 위의 진단 등급도 바뀌지 않습니다.</p>
          <form onSubmit={checkWrittenStyle}>
            <label className="field-label" htmlFor="result-style-text">투자에 대한 생각</label>
            <textarea id="result-style-text" className="field-input model-textarea" value={styleText} onChange={event => setStyleText(event.target.value)} minLength={12} maxLength={1000} placeholder="손실을 어느 정도 감수할 수 있는지, 언제 자금이 필요한지 적어 보세요." required />
            <button type="submit" className="btn btn-secondary" disabled={checkingStyle || styleText.trim().length < 12}>{checkingStyle ? "분석 중…" : "설문과 비교하기"}</button>
          </form>
          {styleError && <p className="error-text" role="alert">{styleError}</p>}
          {styleResult && <div className="model-result" role="status">
            {styleResult.status === "INSUFFICIENT" ? <p>판단하기에 표현이 부족합니다. 손실 수용 정도와 투자 기간을 좀 더 적어 주세요.</p> : <><strong>{styleResult.mismatch ? "설문과 다른 신호가 있습니다" : "설문과 큰 차이가 감지되지 않았습니다"}</strong><p>문장 기준 추정: {styleResult.estimatedProfileLabel ?? "추정 불가"} · {styleResult.estimatedGrade ?? "–"}등급</p></>}
            {styleResult.tags.length > 0 && <div className="tag-row">{styleResult.tags.map(tag => <span key={tag.code} className="tag-chip">{tag.label}</span>)}</div>}
            {styleResult.learningLinks.slice(0, 2).map(link => <p key={link.code} className="subtle">{link.message}</p>)}
            <small>{styleResult.notice}</small>
          </div>}
        </section>}
        <button className="btn btn-secondary btn-block" onClick={() => navigate("/history")}>지난 진단과 비교하기</button>
        <button className="btn btn-secondary btn-block" onClick={() => navigate("/learn")}>안내 범위 밖 상품도 기초 개념 배우기</button>
        <button className="btn btn-primary btn-block" onClick={() => navigate(`/explore/${data.id}`)}>
          탐색 카테고리 보기
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M9 6l6 6-6 6" stroke="#fff" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
        <div style={{ fontSize: 11.5, color: "var(--text-faint)", textAlign: "center" }}>
          본 결과는 참고용 진단이며 투자 자문이 아닙니다.
        </div>
      </div>
    </Shell>
  );
}

/** 진단 때 함께 쓴 서술의 모델 추정. 원문은 저장되지 않으므로 화면에도 원문은 없다. */
function NarrativeSignalCard({ signal }: { signal: NarrativeSignal }) {
  if (signal.status === "INSUFFICIENT") {
    return <section className="card signal-card">
      <span className="eyebrow">자유 서술 신호 · 모델 {signal.modelVersion}</span>
      <h2>서술만으로는 판단을 보류했습니다</h2>
      <p>문장에 손실을 견딜 수 있는 정도나 자금이 필요한 시점이 충분히 드러나지 않아 모델이 성향을 추정하지 않았습니다. 진단 결과에는 영향이 없습니다.</p>
    </section>;
  }
  return <section className={`card signal-card${signal.mismatch ? " is-mismatch" : ""}`}>
    <span className="eyebrow">자유 서술 신호 · 모델 {signal.modelVersion}</span>
    <h2>{signal.mismatch ? "직접 쓴 생각과 설문 답변이 다른 방향을 가리킵니다" : "직접 쓴 생각도 설문 결과와 비슷한 방향입니다"}</h2>
    <p>문장 기준 추정은 <strong>{signal.profileLabel}</strong>(확신도 {Math.round(signal.confidence * 100)}%)입니다.
      {signal.mismatch ? " 설문과 2단계 이상 차이가 있어 위의 '확인해 주세요' 항목에 표시했습니다." : ""} 성향 등급은 설문 답변으로만 정해집니다.</p>
    {signal.tags.length > 0 && <div className="tag-row" aria-label="문장에서 읽은 관심사">{signal.tags.map(tag => <span key={tag.code} className="tag-chip">{tag.label}</span>)}</div>}
    <Link to="/me" className="btn btn-secondary">이 신호로 만든 다음 학습 보기</Link>
  </section>;
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex", flexDirection: "column" }}>
      <Header />
      <Stepper activeIndex={2} />
      <div style={{ flex: 1, display: "flex", justifyContent: "center", padding: "22px 16px 34px" }}>{children}</div>
    </div>
  );
}
