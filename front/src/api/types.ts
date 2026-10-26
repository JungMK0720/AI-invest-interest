// 백엔드 DTO(com.finmate.*.Dtos)와 1:1로 맞춘 타입.
// 필드 이름을 바꿀 때는 반드시 backend의 해당 record도 함께 확인할 것.

export interface UserResponse {
  id: string;
  email: string;
  name: string;
  role?: "USER" | "ADMIN";
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
    narrative: NarrativeSignal | null;
  } | null;
}

/** 진단 제출 시 함께 쓴 자유 서술의 모델 추정. 원문은 서버에 저장되지 않는다. */
export interface NarrativeSignal {
  status: "ESTIMATED" | "INSUFFICIENT";
  profileType: string | null;
  profileLabel: string | null;
  grade: number | null;
  confidence: number;
  mismatch: boolean;
  tags: { code: string; label: string; score: number }[];
  modelVersion: string;
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

export interface LearningLink {
  code: string;
  label: string;
  message: string;
  concepts: { slug: string; title: string }[];
  terms: { slug: string; title: string }[];
}

export interface StyleCheckResponse {
  status: "ESTIMATED" | "INSUFFICIENT";
  estimatedProfileType: string | null;
  estimatedProfileLabel: string | null;
  estimatedGrade: number | null;
  confidence: number | null;
  questionnaireGrade: number;
  mismatch: boolean;
  probabilities: Record<string, number>;
  tags: { code: string; label: string; score: number }[];
  learningLinks: LearningLink[];
  modelVersion: string;
  notice: string;
}

export interface TermHit {
  slug: string;
  term: string;
  categoryId: string;
  definition: string;
  score: number;
  nameMatched: boolean;
}

export interface TermSearchResponse {
  results: TermHit[];
  modelVersion: string;
  indexedTerms: number;
}

export interface ModelCard {
  id: string;
  name: string;
  version: string;
  task: string;
  algorithm: string;
  trainingData: string;
  evaluationData: string;
  metrics: Record<string, number>;
  baselines: Record<string, number>;
  intendedUse: string;
  notIntendedUse: string;
  limitations: string[];
}

export interface ModelsResponse {
  card: { generatedBy: string; models: ModelCard[] };
  runtime: Record<string, string | number | null>;
}

export interface LearningPathStep {
  kind: "REVIEW" | "CONCEPT" | "TERM";
  target: "CONCEPT" | "TERM";
  slug: string;
  title: string;
  reasonCode: string;
  reason: string;
}

export interface LearningPathResponse {
  basis: {
    assessmentId: string;
    profileType: string | null;
    profileLabel: string | null;
    assessedAt: string | null;
    reviewFlags: string[];
    narrativeTags: { code: string; label: string }[];
    narrativeMismatch: boolean;
  } | null;
  reviews: LearningPathStep[];
  concepts: LearningPathStep[];
  terms: LearningPathStep[];
  rulesVersion: string;
  generatedAt: string;
  notice: string;
}

export interface AdminOverview {
  users: number;
  admins: number;
  completedAssessments: number;
  quizAttempts: number;
  publishedTerms: number;
  hiddenTerms: number;
  pendingDrafts: number;
  publishedConcepts: number;
  hiddenConcepts: number;
  pendingConceptDrafts: number;
  glossaryContentVersion: string;
  learningContentVersion: string;
  styleModelVersion: string;
  termModelVersion: string;
  generatedAt: string;
}

export interface AdminRevision {
  id: string;
  action: "CREATED" | "PUBLISHED" | "UNPUBLISHED";
  contentVersion: string;
  editorEmail: string;
  createdAt: string;
}

export interface GlossaryCategory {
  id: string;
  order: number;
  name: string;
  description: string;
  termCount: number;
}

export interface GlossaryTermSummary {
  slug: string;
  term: string;
  english: string | null;
  categoryId: string;
  level: "BASIC" | "INTERMEDIATE" | "ADVANCED";
  definition: string;
}

export interface GlossaryTerm extends GlossaryTermSummary {
  fullName: string | null;
  aliases: string[];
  explanation: string;
  formula: string | null;
  example: string | null;
  caution: string | null;
  related: string[];
  conceptSlug: string | null;
  sourceName: string;
  sourceUrl: string;
  contentVersion: string;
  notice: string;
}

export interface GlossaryQuizQuestion {
  id: string;
  prompt: string;
  choices: { id: string; text: string }[];
}

export interface GlossaryQuiz {
  contentVersion: string;
  questions: GlossaryQuizQuestion[];
  notice: string;
}

export interface GlossaryQuizResult {
  total: number;
  answered: number;
  correctCount: number;
  scorePercent: number;
  items: { questionId: string; selectedChoiceId: string; correctChoiceId: string; correct: boolean; explanation: string }[];
  contentVersion: string;
  notice: string;
}

export interface QuizAttemptSummary {
  id: string;
  slug: string;
  attemptedAt: string;
  contentVersion: string;
  total: number;
  answered: number;
  correctCount: number;
  scorePercent: number;
}

export interface WrongAnswerSummary {
  slug: string;
  target: "CONCEPT" | "TERM";
  title: string;
  questionId: string;
  prompt: string;
  selectedChoiceId: string;
  correctChoiceId: string;
  selectedRationale: string;
  correctRationale: string;
  takeaway: string;
  lastAttemptedAt: string;
  dueAt: string;
  streak: number;
}

export interface ReviewScheduleSummary {
  slug: string;
  target: "CONCEPT" | "TERM";
  title: string;
  dueAt: string;
  dueNow: boolean;
  questionCount: number;
  incorrectCount: number;
}

export interface AdminGlossaryTerm {
  slug: string;
  categoryId: string;
  term: string;
  fullName: string | null;
  english: string | null;
  aliases: string[];
  level: "BASIC" | "INTERMEDIATE" | "ADVANCED";
  definition: string;
  explanation: string;
  formula: string | null;
  example: string | null;
  caution: string | null;
  related: string[];
  conceptSlug: string | null;
  sourceName: string;
  sourceUrl: string;
  status: string;
  contentVersion: string;
  updatedAt: string;
  hasDraft: boolean;
}

export type AdminGlossaryDraft = Pick<AdminGlossaryTerm, "categoryId" | "term" | "fullName" | "english" | "aliases" | "level" | "definition" | "explanation" | "formula" | "example" | "caution" | "related" | "conceptSlug" | "sourceName" | "sourceUrl">;

export interface AdminQuizOption {
  label: string;
  correct: boolean;
  rationale: string;
}

export interface AdminQuizItem {
  id: string;
  prompt: string;
  difficulty: "BASIC" | "INTERMEDIATE" | "ADVANCED";
  options: AdminQuizOption[];
  takeaway: string;
}

/** PUT /admin/learning/concepts/{slug} 본문. 개념 본문과 확인 문제 전체가 한 개정 단위다. */
export interface AdminConceptContent {
  parent: string | null;
  level: number;
  order: number;
  title: string;
  summary: string;
  oneLiner: string;
  explanation: string[];
  keyPoints: string[];
  checklist: string[];
  misconception: string;
  diagramKey: string;
  sourceName: string;
  sourceUrl: string;
  simulationId: string | null;
  quiz: AdminQuizItem[];
}

export interface AdminConcept {
  slug: string;
  content: AdminConceptContent;
  status: "PUBLISHED" | "DRAFT";
  contentVersion: string;
  updatedAt: string;
  hasDraft: boolean;
}

export interface AdminConceptSummary {
  slug: string;
  parent: string | null;
  level: number;
  title: string;
  quizCount: number;
  status: "PUBLISHED" | "DRAFT";
  contentVersion: string;
  updatedAt: string;
  hasDraft: boolean;
}
