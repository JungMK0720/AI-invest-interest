import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";
import Stepper from "../components/Stepper";
import { api, ApiError } from "../api/client";
import type { QuestionResponse } from "../api/types";
import AssessmentAvailability from "../components/AssessmentAvailability";
import type { AvailabilityResponse } from "../api/types";

type LoadState = "loading" | "ready" | "unavailable";

export default function QuestionPage() {
  const navigate = useNavigate();
  const [availability, setAvailability] = useState<AvailabilityResponse | null>(null);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [questions, setQuestions] = useState<QuestionResponse[]>([]);
  const [step, setStep] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [showUnanswered, setShowUnanswered] = useState(false);
  const [narrative, setNarrative] = useState("");

  const load = () => {
    setLoadState("loading");
    Promise.all([api.getQuestions(), api.getAvailability()])
      .then(([qs, available]) => {
        setAvailability(available);
        setQuestions(qs);
        setLoadState(qs.length > 0 ? "ready" : "unavailable");
      })
      .catch(() => setLoadState("unavailable"));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loadState === "loading") {
    return (
      <Shell>
        <div style={{ textAlign: "center", color: "var(--text-muted)", fontSize: 14 }}>불러오는 중...</div>
      </Shell>
    );
  }

  if (loadState === "unavailable") {
    return (
      <Shell>
        <UnavailableState onRetry={load} />
      </Shell>
    );
  }

  if (availability && !availability.canSubmit) {
    return <Shell><div style={{ maxWidth: 640 }}><AssessmentAvailability value={availability} /></div></Shell>;
  }

  // 문항 다음의 마지막 단계는 선택형 자유 서술이다. 건너뛰어도 진단 결과는 같다.
  if (step === questions.length) {
    return (
      <Shell>
        <NarrativeStep
          value={narrative}
          onChange={setNarrative}
          submitting={submitting}
          error={submitError}
          onBack={() => setStep(questions.length - 1)}
          onSubmit={(text) => submit(text)}
        />
      </Shell>
    );
  }

  const current = questions[step];
  const answeredCount = Object.keys(answers).length;
  const isLast = step === questions.length - 1;
  const currentAnswered = !!answers[current.id];

  const selectChoice = (choiceId: string) => {
    setAnswers((prev) => ({ ...prev, [current.id]: choiceId }));
    setShowUnanswered(false);
  };

  const goNext = () => {
    if (!currentAnswered) {
      setShowUnanswered(true);
      return;
    }
    setStep((s) => s + 1);
  };

  async function submit(narrativeText?: string) {
    if (questions.some((q) => !answers[q.id])) {
      setStep(questions.findIndex((q) => !answers[q.id]));
      setShowUnanswered(true);
      return;
    }
    setSubmitting(true);
    setSubmitError(null);
    try {
      const payload = questions.map((q) => ({ questionId: q.id, choiceId: answers[q.id] }));
      const result = await api.submitAssessment(payload, narrativeText);
      navigate(`/result/${result.id}`);
    } catch (err) {
      if (err instanceof ApiError && err.code === "ASSESSMENT_MONTHLY_LIMIT") {
        load();
      } else if (err instanceof ApiError && err.status === 503) {
        setLoadState("unavailable");
      } else if (err instanceof ApiError) {
        setSubmitError(err.message);
      } else {
        setSubmitError("제출을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Shell>
      <div style={{ width: 620, maxWidth: "100%", display: "flex", flexDirection: "column", gap: 20 }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: 13, fontWeight: 700, color: "var(--primary)" }}>
              질문 {step + 1} / {questions.length}
            </span>
            <span style={{ fontSize: 12, color: "var(--text-faint)" }}>답변 완료 {answeredCount} / {questions.length}</span>
          </div>
          <div style={{ width: "100%", height: 7, borderRadius: 6, background: "var(--border)", overflow: "hidden" }}>
            <div
              style={{
                width: `${((step + 1) / questions.length) * 100}%`,
                height: "100%",
                background: "var(--primary)",
                borderRadius: 6,
                transition: "width 0.2s ease",
              }}
            />
          </div>
        </div>

        <div className="card" style={{ padding: 32, display: "flex", flexDirection: "column", gap: 22 }}>
          <div style={{ fontWeight: 700, fontSize: 18, lineHeight: 1.5 }}>{current.content}</div>
          <div role="radiogroup" aria-label={current.content} style={{ display: "flex", flexDirection: "column", gap: 11 }}>
            {current.choices
              .slice()
              .sort((a, b) => a.orderNo - b.orderNo)
              .map((choice) => {
                const selected = answers[current.id] === choice.id;
                return (
                  <button
                    key={choice.id}
                    type="button"
                    role="radio"
                    aria-checked={selected}
                    aria-label={choice.label}
                    onClick={() => selectChoice(choice.id)}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 14,
                      padding: "16px 18px",
                      borderRadius: 12,
                      border: `1.5px solid ${selected ? "var(--primary)" : "var(--border)"}`,
                      background: selected ? "var(--primary-tint)" : "#fff",
                      cursor: "pointer",
                      textAlign: "left",
                      width: "100%",
                    }}
                  >
                    <div
                      style={{
                        width: 19,
                        height: 19,
                        borderRadius: "50%",
                        border: `2px solid ${selected ? "var(--primary)" : "var(--border)"}`,
                        flexShrink: 0,
                        position: "relative",
                      }}
                    >
                      {selected && (
                        <div
                          style={{
                            width: 9,
                            height: 9,
                            borderRadius: "50%",
                            background: "var(--primary)",
                            position: "absolute",
                            top: "50%",
                            left: "50%",
                            transform: "translate(-50%,-50%)",
                          }}
                        />
                      )}
                    </div>
                    <span style={{ fontSize: 14, fontWeight: selected ? 700 : 400 }}>{choice.label}</span>
                  </button>
                );
              })}
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <button className="btn btn-secondary" disabled={step === 0} onClick={() => setStep((s) => s - 1)}>
              이전
            </button>
            <div style={{ display: "flex", gap: 6 }}>
              {questions.map((q, i) => (
                <div
                  key={q.id}
                  style={{
                    width: 7,
                    height: 7,
                    borderRadius: "50%",
                    background: answers[q.id] ? "var(--primary)" : "var(--border)",
                    outline: i === step ? "2px solid var(--primary-tint)" : "none",
                  }}
                />
              ))}
            </div>
            <button className="btn btn-primary" disabled={!currentAnswered} onClick={goNext}>
              {isLast ? "마지막 단계로" : "다음"}
            </button>
          </div>
          {showUnanswered && <div className="error-text" style={{ textAlign: "right" }}>모든 문항에 답변해야 진행할 수 있습니다.</div>}
          {submitError && <div className="error-text" style={{ textAlign: "right" }}>{submitError}</div>}
        </div>
      </div>
    </Shell>
  );
}

const NARRATIVE_LIMIT = 1000;

/**
 * 선택형 자유 서술 단계. 원문은 서버에서 모델 추정에만 쓰고 저장하지 않는다.
 * 설문과 다른 신호가 나오면 결과 화면에 "다시 확인해 보세요" 플래그가 붙을 뿐 등급은 바뀌지 않는다.
 */
function NarrativeStep({ value, onChange, submitting, error, onBack, onSubmit }: {
  value: string;
  onChange: (value: string) => void;
  submitting: boolean;
  error: string | null;
  onBack: () => void;
  onSubmit: (text?: string) => void;
}) {
  const trimmed = value.trim();
  return (
    <div style={{ width: 620, maxWidth: "100%", display: "flex", flexDirection: "column", gap: 20 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <span style={{ fontSize: 13, fontWeight: 700, color: "var(--primary)" }}>마지막 단계 · 선택</span>
        <span style={{ fontSize: 12, color: "var(--text-faint)" }}>모든 문항 답변 완료</span>
      </div>
      <section className="card narrative-step">
        <h2>투자에 대한 생각을 자유롭게 적어 주세요</h2>
        <p className="subtle" style={{ margin: 0 }}>
          손실을 어느 정도 견딜 수 있는지, 언제 돈이 필요한지, 무엇이 걱정되는지 편하게 써 주세요.
          학습된 모델이 문장을 읽고 설문 답변과 다른 신호가 있는지, 어떤 개념부터 배우면 좋을지 알려 드립니다.
        </p>
        <label className="field-label" htmlFor="narrative">나의 투자 생각 (선택)</label>
        <textarea
          id="narrative"
          className="field-input"
          value={value}
          maxLength={NARRATIVE_LIMIT}
          onChange={(event) => onChange(event.target.value)}
          placeholder="예: 작년에 펀드로 손해를 본 뒤로 겁이 많아졌어요. 3년 안에 집 계약금으로 쓸 돈이라 크게 잃고 싶지 않아요."
        />
        <div className="narrative-meta"><span>성향 등급은 위 5개 문항으로만 정해집니다.</span><span>{value.length} / {NARRATIVE_LIMIT}</span></div>
        <p className="privacy-note">입력한 문장은 저장하지 않습니다. 모델이 추정한 성향·관심사 태그·확신도만 이번 진단 기록에 남습니다.</p>
      </section>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
        <button className="btn btn-secondary" onClick={onBack} disabled={submitting}>이전</button>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <button className="btn btn-secondary" disabled={submitting} onClick={() => onSubmit(undefined)}>건너뛰고 제출</button>
          <button className="btn btn-primary" disabled={submitting || trimmed.length < 8} onClick={() => onSubmit(trimmed)}>
            {submitting ? "처리 중..." : "함께 제출하기"}
          </button>
        </div>
      </div>
      {error && <div className="error-text" role="alert" style={{ textAlign: "right" }}>{error}</div>}
    </div>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex", flexDirection: "column" }}>
      <Header />
      <Stepper activeIndex={1} />
      <div style={{ flex: 1, display: "flex", justifyContent: "center", padding: "26px 16px 40px" }}>{children}</div>
    </div>
  );
}

function UnavailableState({ onRetry }: { onRetry: () => void }) {
  return (
    <div style={{ width: 460, display: "flex", flexDirection: "column", alignItems: "center", textAlign: "center", gap: 18, margin: "0 auto" }}>
      <div
        style={{
          width: 72,
          height: 72,
          borderRadius: "50%",
          background: "var(--danger-bg)",
          border: "1.5px solid var(--danger-border)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
          <path d="M12 3l10 18H2L12 3z" stroke="#D64545" strokeWidth="1.8" strokeLinejoin="round" />
          <line x1="12" y1="10" x2="12" y2="14" stroke="#D64545" strokeWidth="1.8" strokeLinecap="round" />
          <circle cx="12" cy="17" r="1" fill="#D64545" />
        </svg>
      </div>
      <div style={{ fontWeight: 700, fontSize: 19 }}>진단 문항을 불러올 수 없습니다</div>
      <div style={{ fontSize: 13.5, color: "var(--text-muted)", lineHeight: 1.7 }}>
        문항 데이터가 아직 준비되지 않았습니다.
        <br />
        답변하신 내용은 유지되며, 잠시 후 다시 시도해 주세요.
      </div>
      <button className="btn btn-primary" onClick={onRetry}>
        다시 시도
      </button>
    </div>
  );
}
