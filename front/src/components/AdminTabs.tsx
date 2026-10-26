import { NavLink } from "react-router-dom";

const TABS = [
  { to: "/admin/glossary", label: "용어" },
  { to: "/admin/learning", label: "학습 개념·확인 문제" },
];

/** 콘텐츠 운영 콘솔의 탭. 용어와 학습 개념은 같은 초안 → 검증 게시 흐름을 따른다. */
export default function AdminTabs() {
  return (
    <nav className="tab-row admin-tabs" aria-label="운영할 콘텐츠">
      {TABS.map(tab => (
        <NavLink key={tab.to} to={tab.to} className={({ isActive }) => `tab-button${isActive ? " is-active" : ""}`}>
          {tab.label}
        </NavLink>
      ))}
    </nav>
  );
}
