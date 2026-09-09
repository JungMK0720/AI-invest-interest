import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { QuizGradeResponse, QuizResponse } from "../api/types";

/**
 * 확인 문제 패널.
 *
 * 정답과 해설은 서버에만 있으므로 채점은 반드시 POST를 거친다. 화면 소스에서 정답이 보이면
 * 확인 문제가 학습 도구로 동작하지 않기 때문이다. 채점 뒤에는 고른 선택지뿐 아니라
 * 모든 선택지의 해설을 보여준다 — 왜 틀렸는지가 왜 맞았는지보다 중요하다.
 */
export default function QuizPanel({ slug }: { slug: string }) {
  const [quiz, setQuiz] = useState<QuizResponse | null>(null);
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [result, setResult] = useState<QuizGradeResponse | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "grading" | "error">("loading");

  useEffect(() => {
    let active = true;
    setQuiz(null);
    setAnswers({});
    setResult(null);
    setStatus("loading");
    api
      .getQuiz(slug)
      .then(data => {
        if (!active) return;
        setQuiz(data);
        setStatus("ready");
      })
      .catch(() => active && setStatus("error"));
    return () => {
      active = false;
    };
  }, [slug]);

  if (status === "loading") return <p role="status">확인 문제를 불러오는 중…</p>;
  if (status === "error")
    return (
      <div role="alert" className="panel-error">
        확인 문제를 불러오지 못했습니다.{" "}
        <button className="btn btn-secondary" onClick={() => setStatus("loading")}>
          다시 시도
        </button>
      </div>
    );
  if (!quiz || quiz.questions.length === 0)
    return <p className="subtle">이 개념에는 아직 확인 문제가 없습니다. 상위 개념의 문제를 먼저 풀어 보세요.</p>;

  const answered = quiz.questions.filter(question => answers[question.id] !== undefined).length;
  const graded = result !== null;

  async function submit() {
    if (!quiz) return;
    setStatus("grading");
    try {
      const payload = quiz.questions.map(question => ({
        questionId: question.id,
        selectedIndex: answers[question.id] ?? -1,
      }));
      setResult(await api.gradeQuiz(quiz.slug, payload));
      setStatus("ready");
    } catch {
      setStatus("error");
    }
  }

  return (
    <section className="quiz-panel" aria-label="확인 문제">
      <header className="quiz-head">
        <div>
          <h3>확인 문제 {quiz.questions.length}문항</h3>
          <p className="subtle">{graded ? result!.summary : "고른 이유를 스스로 설명할 수 있는지 확인해 보세요."}</p>
        </div>
        {graded && (
          <div className={`quiz-score ${result!.scorePercent === 100 ? "is-full" : ""}`}>
            <strong>
              {result!.correctCount}/{result!.total}
            </strong>
            <span>{result!.scorePercent}점</span>
          </div>
        )}
      </header>

      <ol className="quiz-list">
        {quiz.questions.map((question, questionIndex) => {
          const gradedItem = result?.items.find(item => item.questionId === question.id);
          return (
            <li key={question.id} className="quiz-question">
              <p className="quiz-prompt">
                <span className={`quiz-level level-${question.difficulty}`}>{difficultyLabel(question.difficulty)}</span>
                <span>
                  {questionIndex + 1}. {question.prompt}
                </span>
              </p>
              <div className="quiz-options">
                {question.options.map((option, optionIndex) => {
                  const feedback = gradedItem?.options.find(entry => entry.index === optionIndex);
                  const chosen = answers[question.id] === optionIndex;
                  const state = !feedback ? "" : feedback.correct ? " is-correct" : chosen ? " is-wrong" : " is-muted";
                  return (
                    <label key={option} className={`quiz-option${chosen ? " is-chosen" : ""}${state}`}>
                      <input
                        type="radio"
                        name={question.id}
                        checked={chosen}
                        disabled={graded}
                        onChange={() => setAnswers(prev => ({ ...prev, [question.id]: optionIndex }))}
                      />
                      <span className="quiz-option-body">
                        <span className="quiz-option-label">{option}</span>
                        {feedback && <span className="quiz-rationale">{feedback.rationale}</span>}
                      </span>
                    </label>
                  );
                })}
              </div>
              {gradedItem && (
                <p className={`quiz-takeaway${gradedItem.correct ? " is-correct" : ""}`}>
                  <strong>{gradedItem.correct ? "정답" : gradedItem.selectedIndex < 0 ? "미응답" : "다시 보기"}</strong>{" "}
                  {gradedItem.takeaway}
                </p>
              )}
            </li>
          );
        })}
      </ol>

      <div className="action-row">
        {!graded ? (
          <button className="btn btn-primary" disabled={answered === 0 || status === "grading"} onClick={submit}>
            {status === "grading" ? "채점 중…" : `채점하기 (${answered}/${quiz.questions.length} 선택)`}
          </button>
        ) : (
          <button
            className="btn btn-secondary"
            onClick={() => {
              setResult(null);
              setAnswers({});
            }}
          >
            다시 풀기
          </button>
        )}
        <span className="subtle">콘텐츠 버전 {quiz.contentVersion}</span>
      </div>
      <p className="notice-line">{quiz.notice}</p>
    </section>
  );
}

function difficultyLabel(difficulty: string) {
  if (difficulty === "ADVANCED") return "심화";
  if (difficulty === "INTERMEDIATE") return "응용";
  return "기본";
}
