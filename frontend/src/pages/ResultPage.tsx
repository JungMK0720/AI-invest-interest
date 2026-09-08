import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Header from "../components/Header";
import Stepper from "../components/Stepper";
import { api } from "../api/client";
import type { AssessmentResultResponse } from "../api/types";

const GRADE_LABELS = ["공격투자형", "적극투자형", "위험중립형", "안정추구형", "안정형"];
const GRADE_COLORS = ["#E5484D", "#ED7D4C", "#F0B429", "#8FBF3F", "#3FAE64"];

export default function ResultPage() {
  const { assessmentId } = useParams<{ assessmentId: string }>();
  const navigate = useNavigate();
  const [data, setData] = useState<AssessmentResultResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

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

  if (!data || !data.profile) {
    return (
      <Shell>
        <div style={{ textAlign: "center", color: "var(--text-muted)", fontSize: 14 }}>불러오는 중...</div>
      </Shell>
    );
  }

  const { profile } = data;
  const isFallback = profile.reasoningSource === "TEMPLATE";

  return (
    <Shell>
      <div style={{ width: 640, display: "flex", flexDirection: "column", gap: 16 }}>
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
              AI 설명 생성이 지연되어 기본 설명으로 안내합니다. 성향 등급과 탐색 카테고리는 정상적으로 산출되었습니다.
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

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex", flexDirection: "column" }}>
      <Header />
      <Stepper activeIndex={2} />
      <div style={{ flex: 1, display: "flex", justifyContent: "center", padding: "22px 40px 34px" }}>{children}</div>
    </div>
  );
}
