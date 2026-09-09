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
