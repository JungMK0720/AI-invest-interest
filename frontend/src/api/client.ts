import type {
  AnswerItem,
  LearningTreeResponse,
  LearningConceptDetail,
  LearningAiExplainResponse,
  QuizResponse,
  QuizGradeResponse,
  SimulationScenario,
  SimulationRunResponse,
  HistoryResponse,
  AvailabilityResponse,
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

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
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

  getLearningConcepts: () => request<{ slug: string; title: string; summary: string; explanation: string; diagramKey: string; sourceUrl: string }[]>("/learning/concepts"),

  // 학습 트리는 한 번 받아 두고 클라이언트에서 펼치고 접는다.
  getLearningTree: () => request<LearningTreeResponse>("/learning/tree"),

  getLearningConcept: (slug: string) => request<LearningConceptDetail>(`/learning/concepts/${slug}`),

  askLearningConcept: (slug: string, question: string) =>
    request<LearningAiExplainResponse>(`/learning/concepts/${slug}/explain`, {
      method: "POST",
      body: JSON.stringify({ question }),
    }),

  getQuiz: (slug: string) => request<QuizResponse>(`/learning/concepts/${slug}/quiz`),

  // 정답은 서버에만 있으므로 채점은 반드시 이 호출을 거친다.
  gradeQuiz: (slug: string, answers: { questionId: string; selectedIndex: number }[]) =>
    request<QuizGradeResponse>(`/learning/concepts/${slug}/quiz/grade`, {
      method: "POST",
      body: JSON.stringify({ answers }),
    }),

  getConceptSimulations: (slug: string) => request<SimulationScenario[]>(`/learning/concepts/${slug}/simulations`),

  getSimulations: () => request<{ scenarios: SimulationScenario[]; notice: string }>("/learning/simulations"),

  runSimulation: (scenarioId: string, inputs: Record<string, string>, seed?: number) =>
    request<SimulationRunResponse>(`/learning/simulations/${scenarioId}/run`, {
      method: "POST",
      body: JSON.stringify({ inputs, seed: seed ?? null }),
    }),

  submitAssessment: (answers: AnswerItem[]) =>
    request<AssessmentResultResponse>("/assessments", {
      method: "POST",
      body: JSON.stringify({ answers }),
    }),

  getHistory: (page = 0) => request<HistoryResponse>(`/assessments?page=${page}`),
  getAvailability: () => request<AvailabilityResponse>("/assessments/availability"),

  getAssessment: (assessmentId: string) =>
    request<AssessmentResultResponse>(`/assessments/${assessmentId}`),

  getRecommendations: (assessmentId: string) =>
    request<CategoryRecommendationResponse[]>(`/assessments/${assessmentId}/recommendations`),

  getCategories: () => request<CategoryResponse[]>("/product-categories"),

  getProducts: (categoryId?: string) =>
    request<ProductResponse[]>(categoryId ? `/products?categoryId=${categoryId}` : "/products"),

  getProduct: (productId: string) => request<ProductResponse>(`/products/${productId}`),

  getPracticeProducts: () => request<import("./types").PracticeProduct[]>("/practice/products"),
  getPracticeScenarios: () => request<import("./types").PracticeScenario[]>("/practice/scenarios"),
  getPracticeMethodology: () => request<unknown>("/practice/methodology"),
  analyzePractice: (payload: unknown) => request<import("./types").PracticeAnalysis>("/practice/analyze", { method: "POST", body: JSON.stringify(payload) }),
  comparePractice: (payload: unknown) => request<import("./types").PracticeComparison>("/practice/compare", { method: "POST", body: JSON.stringify(payload) }),
};
