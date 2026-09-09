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

  const submit = async () => {
    if (!currentAnswered) {
      setShowUnanswered(true);
      return;
    }
    setSubmitting(true);
    setSubmitError(null);
    try {
      const payload = questions.map((q) => ({ questionId: q.id, choiceId: answers[q.id] }));
      const result = await api.submitAssessment(payload);
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
  };

  return (
    <Shell>
      <div style={{ width: 620, display: "flex", flexDirection: "column", gap: 20 }}>
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
          <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
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
            {isLast ? (
              <button className="btn btn-primary" disabled={!currentAnswered || submitting} onClick={submit}>
                {submitting ? "처리 중..." : "제출하기"}
              </button>
            ) : (
              <button className="btn btn-primary" disabled={!currentAnswered} onClick={goNext}>
                다음
              </button>
            )}
          </div>
          {showUnanswered && <div className="error-text" style={{ textAlign: "right" }}>모든 문항에 답변해야 진행할 수 있습니다.</div>}
          {submitError && <div className="error-text" style={{ textAlign: "right" }}>{submitError}</div>}
        </div>
      </div>
    </Shell>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex", flexDirection: "column" }}>
      <Header />
      <Stepper activeIndex={1} />
      <div style={{ flex: 1, display: "flex", justifyContent: "center", padding: "26px 40px 40px" }}>{children}</div>
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
