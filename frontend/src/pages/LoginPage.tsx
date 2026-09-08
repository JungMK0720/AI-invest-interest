import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { ApiError } from "../api/client";

export default function LoginPage() {
  const [tab, setTab] = useState<"login" | "signup">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { login, signup } = useAuth();
  const navigate = useNavigate();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      if (tab === "login") {
        await login(email, password);
      } else {
        await signup(email, password, name);
      }
      navigate("/start");
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
      else setError("요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex" }}>
      <div
        style={{
          width: 440,
          background: "linear-gradient(155deg,#16224D 0%, #1E3FC4 65%, #2F55E8 100%)",
          padding: "56px 44px",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          position: "relative",
          overflow: "hidden",
        }}
      >
        <svg
          width="360"
          height="360"
          viewBox="0 0 360 360"
          style={{ position: "absolute", right: -120, bottom: -100, opacity: 0.35 }}
        >
          <circle cx="180" cy="180" r="179" stroke="#ffffff" strokeWidth="1" />
          <circle cx="180" cy="180" r="130" stroke="#ffffff" strokeWidth="1" />
          <circle cx="180" cy="180" r="80" stroke="#ffffff" strokeWidth="1" />
        </svg>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 9,
                background: "#ffffff",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none">
                <path
                  d="M6 4h13M6 4v16M6 11h9"
                  stroke="#2F55E8"
                  strokeWidth="2.4"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            <span style={{ fontWeight: 900, fontSize: 20, color: "#fff" }}>FinMATE</span>
          </div>
          <div style={{ marginTop: 28, fontWeight: 700, fontSize: 26, color: "#fff", lineHeight: 1.4 }}>
            AI가 분석하는 나의
            <br />
            투자 성향, 근거 있는
            <br />
            탐색까지
          </div>
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          {[
            "5문항 진단으로 5단계 투자 성향 확인",
            "AI가 생성하는 근거 문장으로 쉽게 이해",
            "내부 위험 필터를 통과한 카테고리·상품 탐색",
          ].map((t) => (
            <div key={t} style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" style={{ marginTop: 2, flexShrink: 0 }}>
                <path d="M5 13l4 4L19 7" stroke="#BFD0FF" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              <span style={{ fontSize: 13.5, color: "#DCE4FF", lineHeight: 1.5 }}>{t}</span>
            </div>
          ))}
          <div style={{ fontSize: 11.5, color: "#9FB4EE", marginTop: 6 }}>
            본 서비스는 투자 자문·투자 권유가 아닌 탐색 정보 제공 서비스입니다.
          </div>
        </div>
      </div>

      <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: 40 }}>
        <form onSubmit={submit} style={{ width: 360, display: "flex", flexDirection: "column", gap: 22 }}>
          <div style={{ display: "flex", borderBottom: "1.5px solid var(--border)" }}>
            <button
              type="button"
              onClick={() => setTab("login")}
              style={{
                background: "none",
                border: "none",
                padding: "0 0 14px 0",
                marginRight: 28,
                fontWeight: tab === "login" ? 700 : 500,
                fontSize: 15,
                color: tab === "login" ? "var(--primary)" : "var(--text-faint)",
                borderBottom: tab === "login" ? "2.5px solid var(--primary)" : "2.5px solid transparent",
                marginBottom: -1.5,
              }}
            >
              로그인
            </button>
            <button
              type="button"
              onClick={() => setTab("signup")}
              style={{
                background: "none",
                border: "none",
                padding: "0 0 14px 0",
                fontWeight: tab === "signup" ? 700 : 500,
                fontSize: 15,
                color: tab === "signup" ? "var(--primary)" : "var(--text-faint)",
                borderBottom: tab === "signup" ? "2.5px solid var(--primary)" : "2.5px solid transparent",
                marginBottom: -1.5,
              }}
            >
              회원가입
            </button>
          </div>

          {tab === "signup" && (
            <div>
              <label className="field-label">이름</label>
              <input
                className="field-input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="홍길동"
                required
              />
            </div>
          )}
          <div>
            <label className="field-label">이메일</label>
            <input
              className="field-input"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
            />
          </div>
          <div>
            <label className="field-label">비밀번호</label>
            <input
              className="field-input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="8자 이상"
              minLength={8}
              required
            />
          </div>

          {error && <div className="error-text">{error}</div>}

          <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
            {loading ? "처리 중..." : tab === "login" ? "로그인" : "가입하기"}
          </button>

          <div style={{ textAlign: "center", fontSize: 13, color: "var(--text-muted)" }}>
            {tab === "login" ? (
              <>
                계정이 없으신가요?{" "}
                <a href="#" onClick={(e) => { e.preventDefault(); setTab("signup"); }}>
                  회원가입
                </a>
              </>
            ) : (
              <>
                이미 계정이 있으신가요?{" "}
                <a href="#" onClick={(e) => { e.preventDefault(); setTab("login"); }}>
                  로그인
                </a>
              </>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
