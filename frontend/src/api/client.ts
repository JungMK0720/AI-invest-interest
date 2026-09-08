import type {
  AnswerItem,
  ApiErrorBody,
  AssessmentResultResponse,
  CategoryRecommendationResponse,
  CategoryResponse,
  LoginResponse,
  ProductResponse,
  QuestionResponse,
  UserResponse,
} from "./types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";
const TOKEN_KEY = "finmate.accessToken";

export class ApiError extends Error {
  code: string;
  status: number;
  constructor(status: number, body: ApiErrorBody) {
    super(body.message);
    this.code = body.code;
    this.status = status;
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string> | undefined),
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (!res.ok) {
    let body: ApiErrorBody;
    try {
      body = await res.json();
    } catch {
      body = { code: "UNKNOWN", message: "요청을 처리하지 못했습니다." };
    }
    throw new ApiError(res.status, body);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  signup: (email: string, password: string, name: string) =>
    request<UserResponse>("/auth/signup", {
      method: "POST",
      body: JSON.stringify({ email, password, name }),
    }),

  login: (email: string, password: string) =>
    request<LoginResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),

  getQuestions: () => request<QuestionResponse[]>("/questions"),

  submitAssessment: (answers: AnswerItem[]) =>
    request<AssessmentResultResponse>("/assessments", {
      method: "POST",
      body: JSON.stringify({ answers }),
    }),

  getAssessment: (assessmentId: string) =>
    request<AssessmentResultResponse>(`/assessments/${assessmentId}`),

  getRecommendations: (assessmentId: string) =>
    request<CategoryRecommendationResponse[]>(`/assessments/${assessmentId}/recommendations`),

  getCategories: () => request<CategoryResponse[]>("/product-categories"),

  getProducts: (categoryId?: string) =>
    request<ProductResponse[]>(categoryId ? `/products?categoryId=${categoryId}` : "/products"),

  getProduct: (productId: string) => request<ProductResponse>(`/products/${productId}`),
};
