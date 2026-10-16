import { useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const PUBLIC_LINKS = [
  { to: "/glossary", label: "용어 사전" },
  { to: "/learn", label: "개념 학습" },
  { to: "/practice", label: "모의투자" },
  { to: "/models", label: "모델 실험실" },
];

/**
 * 상단 메뉴. 학습 메뉴는 누구에게나 보이고, 진단·내 학습은 로그인 후, 콘텐츠 관리는 관리자에게만 보인다.
 * 좁은 화면에서는 메뉴를 접어 두고 버튼으로 펼친다.
 */
export default function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const links = [
    ...PUBLIC_LINKS,
    ...(user ? [{ to: "/start", label: "성향 진단" }, { to: "/me", label: "내 학습" }] : []),
    ...(user?.role === "ADMIN" ? [{ to: "/admin/glossary", label: "콘텐츠 관리" }] : []),
  ];

  return (
    <header className="site-header">
      <div className="site-header-inner">
        <Link to="/" className="brand" aria-label="FinMATE 홈">
          <span className="brand-mark" aria-hidden="true">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
              <path d="M6 4h13M6 4v16M6 11h9" stroke="#fff" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
          <span className="brand-name">FinMATE</span>
        </Link>

        <button
          type="button"
          className="nav-toggle-button"
          aria-expanded={open}
          aria-controls="site-nav"
          onClick={() => setOpen(value => !value)}
        >
          {open ? "메뉴 닫기" : "메뉴"}
        </button>

        <nav id="site-nav" className={`site-nav${open ? " is-open" : ""}`} aria-label="주요 메뉴">
          {links.map(link => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => `site-nav-link${isActive ? " is-active" : ""}`}
              onClick={() => setOpen(false)}
            >
              {link.label}
            </NavLink>
          ))}
          <span className="site-nav-account">
            {user ? (
              <>
                <span className="header-name">{user.name} 님</span>
                <span className="avatar" aria-hidden="true">{user.name.charAt(0)}</span>
                <button
                  type="button"
                  className="btn btn-secondary btn-small"
                  onClick={async () => {
                    try { await logout(); } catch { /* 로컬 세션은 이미 정리됨 */ }
                    navigate("/");
                  }}
                >
                  로그아웃
                </button>
              </>
            ) : (
              <button type="button" className="btn btn-primary btn-small" onClick={() => navigate("/login")}>
                로그인
              </button>
            )}
          </span>
        </nav>
      </div>
    </header>
  );
}
