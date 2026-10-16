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
  StyleCheckResponse,
  TermSearchResponse,
  GlossaryCategory,
  GlossaryTermSummary,
  GlossaryTerm,
  GlossaryQuiz,
  GlossaryQuizResult,
  QuizAttemptSummary,
  WrongAnswerSummary,
  ReviewScheduleSummary,
  AdminGlossaryTerm,
  AdminGlossaryDraft,
  AdminOverview,
  AdminRevision,
  AdminConcept,
  AdminConceptContent,
  AdminConceptSummary,
  LearningPathResponse,
  ModelsResponse,
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

  getMe: () => request<UserResponse>("/auth/me"),
  logout: () => request<void>("/auth/logout", { method: "POST" }),

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

  // narrative는 선택 입력이다. 서버는 모델 추정 결과만 저장하고 원문은 버린다.
  submitAssessment: (answers: AnswerItem[], narrative?: string) =>
    request<AssessmentResultResponse>("/assessments", {
      method: "POST",
      body: JSON.stringify({ answers, narrative: narrative?.trim() ? narrative.trim() : null }),
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

  checkStyle: (text: string, questionnaireGrade: number) =>
    request<StyleCheckResponse>("/ml/style-check", {
      method: "POST",
      body: JSON.stringify({ text, questionnaireGrade }),
    }),

  searchTermsByQuestion: (question: string, limit = 8) =>
    request<TermSearchResponse>("/ml/term-search", {
      method: "POST",
      body: JSON.stringify({ question, limit }),
    }),

  getGlossaryCategories: () =>
    request<{ contentVersion: string; categories: GlossaryCategory[] }>("/glossary/categories"),
  getGlossaryTerms: (filters: { q?: string; category?: string; level?: string } = {}) => {
    const params = new URLSearchParams();
    if (filters.q) params.set("q", filters.q);
    if (filters.category) params.set("category", filters.category);
    if (filters.level) params.set("level", filters.level);
    const query = params.toString();
    return request<{ contentVersion: string; total: number; terms: GlossaryTermSummary[] }>(`/glossary/terms${query ? `?${query}` : ""}`);
  },
  getGlossaryTerm: (slug: string) => request<GlossaryTerm>(`/glossary/terms/${encodeURIComponent(slug)}`),
  // seed가 같으면 같은 문제가 나온다. "새 문제"마다 seed를 바꿔 다른 용어를 뽑는다.
  getGlossaryQuiz: (category?: string, count = 5, seed = Math.floor(Math.random() * 2_147_483_647)) => {
    const params = new URLSearchParams({ count: String(count), seed: String(seed) });
    if (category) params.set("category", category);
    return request<GlossaryQuiz>(`/glossary/quiz?${params}`);
  },
  gradeGlossaryQuiz: (quiz: GlossaryQuiz, answers: { questionId: string; choiceId: string }[]) =>
    request<GlossaryQuizResult>("/glossary/quiz/grade", {
      method: "POST",
      body: JSON.stringify({ contentVersion: quiz.contentVersion, answers }),
    }),
  getGlossaryBookmarks: () => request<{ terms: GlossaryTermSummary[] }>("/glossary/bookmarks"),
  addGlossaryBookmark: (slug: string) => request<void>(`/glossary/bookmarks/${encodeURIComponent(slug)}`, { method: "PUT" }),
  removeGlossaryBookmark: (slug: string) => request<void>(`/glossary/bookmarks/${encodeURIComponent(slug)}`, { method: "DELETE" }),
  getQuizAttempts: () => request<{ items: QuizAttemptSummary[] }>("/me/quiz-attempts"),
  getWrongAnswers: () => request<{ items: WrongAnswerSummary[] }>("/me/wrong-answers"),
  getReviewSchedule: () => request<{ items: ReviewScheduleSummary[] }>("/me/review-schedule"),
  getAdminGlossaryTerms: () => request<{ items: AdminGlossaryTerm[] }>("/admin/glossary/terms"),
  saveAdminGlossaryTerm: (slug: string, draft: AdminGlossaryDraft) =>
    request<AdminGlossaryTerm>(`/admin/glossary/terms/${encodeURIComponent(slug)}`, {
      method: "PUT",
      body: JSON.stringify(draft),
    }),
  publishAdminGlossaryTerm: (slug: string) =>
    request<AdminGlossaryTerm>(`/admin/glossary/terms/${encodeURIComponent(slug)}/publish`, { method: "POST" }),
  unpublishAdminGlossaryTerm: (slug: string) =>
    request<AdminGlossaryTerm>(`/admin/glossary/terms/${encodeURIComponent(slug)}/unpublish`, { method: "POST" }),
  discardAdminGlossaryDraft: (slug: string) =>
    request<void>(`/admin/glossary/terms/${encodeURIComponent(slug)}/draft`, { method: "DELETE" }),
  createAdminGlossaryTerm: (slug: string, content: AdminGlossaryDraft) =>
    request<AdminGlossaryTerm>("/admin/glossary/terms", {
      method: "POST",
      body: JSON.stringify({ slug, content }),
    }),
  getAdminGlossaryRevisions: (slug: string) =>
    request<{ slug: string; items: AdminRevision[] }>(`/admin/glossary/terms/${encodeURIComponent(slug)}/revisions`),
  getAdminOverview: () => request<AdminOverview>("/admin/overview"),
  getAdminConcepts: () =>
    request<{ learningContentVersion: string; items: AdminConceptSummary[] }>("/admin/learning/concepts"),
  getAdminConcept: (slug: string) => request<AdminConcept>(`/admin/learning/concepts/${encodeURIComponent(slug)}`),
  saveAdminConcept: (slug: string, content: AdminConceptContent) =>
    request<AdminConcept>(`/admin/learning/concepts/${encodeURIComponent(slug)}`, {
      method: "PUT",
      body: JSON.stringify(content),
    }),
  publishAdminConcept: (slug: string) =>
    request<AdminConcept>(`/admin/learning/concepts/${encodeURIComponent(slug)}/publish`, { method: "POST" }),
  unpublishAdminConcept: (slug: string) =>
    request<AdminConcept>(`/admin/learning/concepts/${encodeURIComponent(slug)}/unpublish`, { method: "POST" }),
  discardAdminConceptDraft: (slug: string) =>
    request<void>(`/admin/learning/concepts/${encodeURIComponent(slug)}/draft`, { method: "DELETE" }),
  getAdminConceptRevisions: (slug: string) =>
    request<{ slug: string; items: AdminRevision[] }>(`/admin/learning/concepts/${encodeURIComponent(slug)}/revisions`),

  getModels: () => request<ModelsResponse>("/ml/models"),
  getLearningPath: () => request<LearningPathResponse>("/me/learning-path"),
};
