import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "0 40px",
        height: 64,
        borderBottom: "1px solid var(--border)",
        background: "var(--surface)",
        flexShrink: 0,
      }}
    >
      <div
        style={{ display: "flex", alignItems: "center", gap: 10, cursor: "pointer" }}
        onClick={() => navigate("/")}
      >
        <div
          style={{
            width: 28,
            height: 28,
            borderRadius: 8,
            background: "var(--primary)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
            <path
              d="M6 4h13M6 4v16M6 11h9"
              stroke="#fff"
              strokeWidth="2.4"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <span style={{ fontWeight: 700, fontSize: 16.5, color: "var(--text)", letterSpacing: "-0.2px" }}>
          FinMATE
        </span>
      </div>

      {user && (
        <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
          <button className="btn btn-secondary" onClick={() => navigate("/learn")}>상품 학습</button>
          <button className="btn btn-secondary" onClick={() => navigate("/history")}>진단 이력</button>
          <button className="btn btn-secondary" onClick={() => navigate("/practice")}>모의투자</button>
          <span className="header-name" style={{ fontSize: 13, color: "var(--text-muted)" }}>{user.name} 님</span>
          <div
            style={{
              width: 30,
              height: 30,
              borderRadius: "50%",
              background: "var(--primary-tint)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontWeight: 700,
              fontSize: 13,
              color: "var(--primary)",
            }}
          >
            {user.name.charAt(0)}
          </div>
          <button
            className="btn btn-secondary"
            style={{ padding: "8px 14px", fontSize: 12.5 }}
            onClick={() => {
              logout();
              navigate("/login");
            }}
          >
            로그아웃
          </button>
        </div>
      )}
    </header>
  );
}
