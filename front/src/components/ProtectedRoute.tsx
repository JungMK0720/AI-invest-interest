import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/** 로그인이 필요한 화면. 로그인 후 원래 가려던 주소로 돌아올 수 있게 현재 위치를 넘긴다. */
export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, checkingSession } = useAuth();
  const location = useLocation();
  if (checkingSession) return <p role="status" className="subtle" style={{ padding: 24 }}>로그인 상태를 확인하는 중…</p>;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />;
  return <>{children}</>;
}
