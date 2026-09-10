// 백엔드 DTO(com.finmate.*.Dtos)와 1:1로 맞춘 타입.
// 필드 이름을 바꿀 때는 반드시 backend의 해당 record도 함께 확인할 것.

export interface UserResponse {
  id: string;
  email: string;
  name: string;
}

export interface LoginResponse {
  accessToken: string;
  user: UserResponse;
}

export interface ChoiceResponse {
  id: string;
  label: string;
  orderNo: number;
}

export interface QuestionResponse {
  id: string;
  content: string;
  orderNo: number;
  choices: ChoiceResponse[];
}

export interface AnswerItem {
  questionId: string;
  choiceId: string;
}

export type ReasoningSource = "LLM" | "TEMPLATE";

export interface ProfileResponse {
  profileType: string;
  profileLabel: string;
  profileGrade: number;
  totalScore: number;
  reasoning: string;
  reasoningSource: ReasoningSource;
}

export type AssessmentStatus = "PROCESSING" | "COMPLETED" | "FAILED";

export interface AssessmentResultResponse {
  id: string;
  status: AssessmentStatus;
  profile: ProfileResponse | null;
  createdAt: string;
  completedAt: string | null;
  evidence: {
    ruleVersion: string;
    questionnaireVersion: string;
    answers: { questionId: string; question: string; answer: string }[];
    reviewFlags: { code: string; message: string }[];
  } | null;
}

export interface CategoryRecommendationResponse {
  categoryId: string;
  categoryName: string;
  riskGrade: number;
  riskGradeLabel: string;
  rank: number;
  reason: string;
}

export interface CategoryResponse {
  id: string;
  name: string;
  riskGrade: number;
  riskGradeLabel: string;
  description: string;
}

export interface ProductResponse {
  id: string;
  categoryId: string;
  categoryName: string;
  name: string;
  riskGrade: number;
  riskGradeLabel: string;
  expectedPeriod: string;
  description: string;
  disclaimer: string;
}

export interface ApiErrorBody {
  code: string;
  message: string;
}

export interface HistoryResponse {
  items: AssessmentResultResponse[];
  page: number;
  hasNext: boolean;
}
export interface AvailabilityResponse {
  canSubmit: boolean;
  policyVersion: string;
  nextAvailableAt: string | null;
  currentAssessmentId: string | null;
  currentStatus: AssessmentStatus | null;
}

// ---------------------------------------------------------------- 학습 도메인
// backend com.finmate.learning.LearningDtos / SimulationDtos와 1:1로 맞춘 타입.

/** 좌측 개념 트리의 한 노드. children을 그대로 품는다. */
export interface LearningTreeNode {
  slug: string;
  title: string;
  summary: string;
  level: number;
  hasSimulation: boolean;
  quizCount: number;
  descendantCount: number;
  children: LearningTreeNode[];
}

export interface LearningTreeResponse {
  contentVersion: string;
  conceptCount: number;
  roots: LearningTreeNode[];
  notice: string;
}

export interface LearningCrumb {
  slug: string;
  title: string;
  level: number;
}

export interface LearningConceptDetail {
  slug: string;
  title: string;
  summary: string;
  oneLiner: string;
  level: number;
  explanation: string[];
  keyPoints: string[];
  checklist: string[];
  misconception: string | null;
  diagramKey: string;
  sourceName: string;
  sourceUrl: string;
  simulationId: string | null;
  quizCount: number;
  breadcrumb: LearningCrumb[];
  children: LearningTreeNode[];
  relatedSlugs: string[];
  contentVersion: string;
  notice: string;
}

export interface LearningAiExplainResponse {
  slug: string;
  title: string;
  answer: string;
  source: "CLAUDE" | "TEMPLATE";
  sourceName: string;
  sourceUrl: string;
  contentVersion: string;
  notice: string;
}

/** 정답이 제거된 확인 문제. 채점은 서버에서만 한다. */
export interface QuizQuestion {
  id: string;
  prompt: string;
  difficulty: "BASIC" | "INTERMEDIATE" | "ADVANCED";
  options: string[];
}

export interface QuizResponse {
  slug: string;
  title: string;
  contentVersion: string;
  questions: QuizQuestion[];
  notice: string;
}

export interface QuizOptionFeedback {
  index: number;
  label: string;
  correct: boolean;
  rationale: string;
}

export interface QuizGradedItem {
  questionId: string;
  prompt: string;
  selectedIndex: number;
  correctIndex: number;
  correct: boolean;
  options: QuizOptionFeedback[];
  takeaway: string;
}

export interface QuizGradeResponse {
  slug: string;
  total: number;
  correctCount: number;
  scorePercent: number;
  contentVersion: string;
  items: QuizGradedItem[];
  summary: string;
}

export type SimulationFieldType = "NUMBER" | "CHOICE";

export interface SimulationChoice {
  value: string;
  label: string;
}

export interface SimulationInputField {
  key: string;
  label: string;
  type: SimulationFieldType;
  unit: string | null;
  min: number | null;
  max: number | null;
  step: number | null;
  defaultValue: string;
  choices: SimulationChoice[];
  help: string;
}

export interface SimulationScenario {
  id: string;
  title: string;
  question: string;
  summary: string;
  mechanism: string;
  conceptSlugs: string[];
  inputs: SimulationInputField[];
  randomised: boolean;
  notice: string;
}

export type SimulationTone = "GOOD" | "BAD" | "NEUTRAL";

export interface SimulationMetric {
  label: string;
  value: string;
  rawValue: number;
  unit: string | null;
  tone: SimulationTone;
  note: string;
}

export interface SimulationPoint {
  label: string;
  value: number;
}

export interface SimulationSeries {
  name: string;
  kind: "line" | "bar" | "waterfall";
  unit: string;
  points: SimulationPoint[];
}

export interface SimulationTable {
  title: string;
  columns: string[];
  rows: { label: string; cells: string[] }[];
  note: string;
}

export interface SimulationRunResponse {
  scenarioId: string;
  title: string;
  headline: string;
  metrics: SimulationMetric[];
  series: SimulationSeries[];
  tables: SimulationTable[];
  lessons: string[];
  assumptions: string[];
  echo: Record<string, string>;
  seed: number;
  notice: string;
}

export interface PracticeProduct {
  id: string; name: string; assetClass: string; mechanism: string; driver: string;
  duration: number; annualFeePercent: number; leverage: number; foreignCurrency: boolean;
  conceptSlug: string; explanation: string;
}
export interface PracticeScenario {
  id: string; name: string; description: string;
  shocks: { equityPercent: number; ratePoints: number; creditSpreadPoints: number; goldPercent: number; realEstatePercent: number; fxPercent: number; days: number };
}
export interface PracticeEffect { label: string; amount: number; formula: string; meaning: string; }
export interface PracticePosition { rowId: string; productId: string; label: string; assetClass: string; conceptSlug: string; invested: number; weightPercent: number; economicExposure: number; endingValue: number; profitLoss: number; returnPercent: number; effects: PracticeEffect[]; observations: string[]; }
export interface PracticeAnalysis { modelVersion: string; budget: number; unallocatedCash: number; endingValue: number; profitLoss: number; returnPercent: number; linearExposureRatio: number; optionPremium: number; allocation: { assetClass: string; amount: number; weightPercent: number }[]; positions: PracticePosition[]; lessons: string[]; limitations: string[]; }
export interface PracticeComparison { before: PracticeAnalysis; after: PracticeAnalysis; profitLossDifference: number; interpretation: string; }
