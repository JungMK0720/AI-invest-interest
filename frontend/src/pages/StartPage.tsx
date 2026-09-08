import { useNavigate } from "react-router-dom";
import Header from "../components/Header";
import Stepper from "../components/Stepper";

const LAST_ASSESSMENT_KEY = "finmate.lastAssessmentId";

export default function StartPage() {
  const navigate = useNavigate();
  const lastAssessmentId = localStorage.getItem(LAST_ASSESSMENT_KEY);

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex", flexDirection: "column" }}>
      <Header />
      <Stepper activeIndex={0} />

      <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
        <div
          className="card"
          style={{
            width: 520,
            padding: "48px 44px",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            textAlign: "center",
            gap: 22,
            boxShadow: "0 2px 10px rgba(21,27,46,0.05)",
          }}
        >
          <div
            style={{
              width: 76,
              height: 76,
              borderRadius: 20,
              background: "var(--primary-tint)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <svg width="38" height="38" viewBox="0 0 24 24" fill="none">
              <rect x="5" y="3" width="14" height="18" rx="2" stroke="#2F55E8" strokeWidth="1.8" />
              <path d="M9 8h6M9 12h6M9 16h3.5" stroke="#2F55E8" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
          </div>
          <div style={{ fontWeight: 700, fontSize: 21 }}>투자 성향 진단을 시작해볼까요?</div>
          <div style={{ fontSize: 14, color: "var(--text-muted)", lineHeight: 1.6 }}>
            5개의 질문에 답하면 AI가 회원님의 투자 성향과
            <br />
            탐색 가능한 금융상품 카테고리를 안내합니다.
            <br />
            약 2분이 소요됩니다.
          </div>
          <button className="btn btn-primary btn-block" style={{ marginTop: 8 }} onClick={() => navigate("/diagnosis")}>
            진단 시작하기
          </button>
          {lastAssessmentId && (
            <a
              href="#"
              style={{ fontSize: 13, display: "flex", alignItems: "center", gap: 5 }}
              onClick={(e) => {
                e.preventDefault();
                navigate(`/result/${lastAssessmentId}`);
              }}
            >
              지난 진단 결과 다시 보기
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                <path d="M9 6l6 6-6 6" stroke="#2F55E8" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </a>
          )}
          <div
            style={{
              fontSize: 11.5,
              color: "var(--text-faint)",
              borderTop: "1px solid var(--border)",
              paddingTop: 16,
              width: "100%",
            }}
          >
            이 진단은 투자 자문이 아닌 탐색 정보 제공을 목적으로 합니다.
          </div>
        </div>
      </div>
    </div>
  );
}

export { LAST_ASSESSMENT_KEY };
