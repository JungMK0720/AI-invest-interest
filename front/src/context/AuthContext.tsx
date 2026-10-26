import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, getToken, setToken } from "../api/client";
import type { UserResponse } from "../api/types";

const USER_KEY = "finmate.user";

interface AuthContextValue {
  user: UserResponse | null;
  isAuthenticated: boolean;
  checkingSession: boolean;
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, name: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadStoredUser(): UserResponse | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserResponse;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(loadStoredUser);
  const [checkingSession, setCheckingSession] = useState(!!getToken());

  useEffect(() => {
    const token = getToken();
    if (!token) {
      localStorage.removeItem(USER_KEY);
      setUser(null);
      setCheckingSession(false);
      return;
    }
    let current = true;
    api.getMe().then(freshUser => {
      if (!current || getToken() !== token) return;
      localStorage.setItem(USER_KEY, JSON.stringify(freshUser));
      setUser(freshUser);
    }).catch(() => {
      if (!current || getToken() !== token) return;
      setToken(null);
      localStorage.removeItem(USER_KEY);
      setUser(null);
    }).finally(() => { if (current) setCheckingSession(false); });
    return () => { current = false; };
  }, []);

  const login = async (email: string, password: string) => {
    const res = await api.login(email, password);
    setToken(res.accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    setUser(res.user);
  };

  const signup = async (email: string, password: string, name: string) => {
    await api.signup(email, password, name);
    await login(email, password);
  };

  const logout = async () => {
    try {
      if (getToken()) await api.logout();
    } finally {
      setToken(null);
      localStorage.removeItem(USER_KEY);
      setUser(null);
      setCheckingSession(false);
    }
  };

  const value = useMemo(
    () => ({ user, isAuthenticated: !!user && !!getToken(), checkingSession, login, signup, logout }),
    [user, checkingSession],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.");
  return ctx;
}
