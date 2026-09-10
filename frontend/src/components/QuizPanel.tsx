import { useEffect, useRef, useState } from "react";
import { request } from "../api/client";

type Choice = { id: string; text: string };
type Question = { id: string; prompt: string; difficulty: string; options: string[]; choices?: Choice[] };
type Quiz = { slug: string; title: string; contentVersion: string; questions: Question[]; notice: string };
type AttemptItem = { questionId: string; prompt: string; selectedChoiceId: string | null; correctChoiceId: string; correct: boolean; choices: { id: string; text: string; correct: boolean; rationale: string }[]; takeaway: string };
type Attempt = { total: number; answered: number; correctCount: number; scorePercent: number; items: AttemptItem[]; summary: string; contentVersion: string; notice: string };

/** 서버가 정답을 숨긴 문제를 읽고, ID 기반으로 제출하는 학습 확인 패널. */
export default function QuizPanel({ slug }: { slug: string }) {
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [result, setResult] = useState<Attempt | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "grading" | "error">("loading");
  const [errorMessage, setErrorMessage] = useState("");
  const [retry, setRetry] = useState(0);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    const controller = new AbortController();
    setStatus("loading"); setErrorMessage(""); setQuiz(null); setResult(null); setAnswers({});
    request<Quiz>(`/learning/concepts/${encodeURIComponent(slug)}/quiz`, { signal: controller.signal })
      .then(data => { if (mounted.current) { setQuiz(data); setStatus("ready"); } })
      .catch(error => { if (mounted.current && error?.name !== "AbortError") { setStatus("error"); setErrorMessage("확인 문제를 불러오지 못했습니다."); } });
    return () => { mounted.current = false; controller.abort(); };
  }, [slug, retry]);

  if (status === "loading") return <p role="status">확인 문제를 불러오는 중…</p>;
  if (status === "error") return <div role="alert" className="panel-error">{errorMessage}{" "}<button className="btn btn-secondary" onClick={() => setRetry(value => value + 1)}>다시 시도</button></div>;
  if (!quiz || quiz.questions.length === 0) return <p className="subtle">이 개념에는 아직 확인 문제가 없습니다. 상위 개념의 문제를 먼저 풀어 보세요.</p>;
  const currentQuiz = quiz;

  const answered = Object.keys(answers).length;
  const graded = result !== null;
  async function submit() {
    setStatus("grading"); setErrorMessage("");
    try {
      const attempt = await request<Attempt>(`/learning/concepts/${encodeURIComponent(slug)}/quiz/attempts`, { method: "POST", body: JSON.stringify({ contentVersion: currentQuiz.contentVersion, answers: Object.entries(answers).map(([questionId, choiceId]) => ({ questionId, choiceId })) }) });
      if (mounted.current) { setResult(attempt); setStatus("ready"); }
    } catch (error: any) {
      if (mounted.current) { setStatus("ready"); setErrorMessage(error?.message || "채점하지 못했습니다. 잠시 후 다시 시도해 주세요."); }
    }
  }

  return <section className="quiz-panel" aria-label="확인 문제">
    <header className="quiz-head"><div><h3>{quiz.title} 확인 문제 {quiz.questions.length}문항</h3><p className="subtle" role="status">{graded ? result.summary : errorMessage || "고른 이유를 스스로 설명할 수 있는지 확인해 보세요."}</p></div>{graded && <div className={`quiz-score ${result.scorePercent === 100 ? "is-full" : ""}`}><strong>{result.correctCount}/{result.total}</strong><span>{result.scorePercent}점</span></div>}</header>
    <ol className="quiz-list">{quiz.questions.map((question, questionIndex) => { const choices = question.choices ?? question.options.map((text, index) => ({ id: `${question.id}-choice-${index}`, text })); const gradedItem = result?.items.find(item => item.questionId === question.id); return <li key={question.id} className="quiz-question"><fieldset disabled={graded || status === "grading"}><legend className="quiz-prompt"><span className={`quiz-level level-${question.difficulty}`}>{difficultyLabel(question.difficulty)}</span><span>{questionIndex + 1}. {question.prompt}</span></legend><div className="quiz-options">{choices.map(choice => { const feedback = gradedItem?.choices.find(item => item.id === choice.id); const chosen = answers[question.id] === choice.id; const state = !feedback ? "" : feedback.correct ? " is-correct" : chosen ? " is-wrong" : " is-muted"; return <label key={choice.id} className={`quiz-option${chosen ? " is-chosen" : ""}${state}`}><input type="radio" name={question.id} checked={chosen} onChange={() => setAnswers(prev => ({ ...prev, [question.id]: choice.id }))}/><span className="quiz-option-body"><span className="quiz-option-label">{choice.text}</span>{feedback && <span className="quiz-rationale">{feedback.rationale}</span>}</span></label>; })}</div></fieldset>{gradedItem && <p className={`quiz-takeaway${gradedItem.correct ? " is-correct" : ""}`}><strong>{gradedItem.correct ? "정답" : gradedItem.selectedChoiceId === null ? "미응답" : "다시 보기"}</strong>{" "}{gradedItem.takeaway}</p>}</li>; })}</ol>
    <div className="action-row">{!graded ? <button className="btn btn-primary" disabled={answered === 0 || status === "grading"} onClick={submit}>{status === "grading" ? "채점 중…" : `채점하기 (${answered}/${quiz.questions.length} 선택)`}</button> : <button className="btn btn-secondary" onClick={() => { setResult(null); setAnswers({}); setErrorMessage(""); }}>다시 풀기</button>}<span className="subtle">콘텐츠 버전 {quiz.contentVersion}</span></div><p className="notice-line">{quiz.notice}</p>
  </section>;
}

function difficultyLabel(value: string) { if (value === "ADVANCED") return "심화"; if (value === "INTERMEDIATE") return "응용"; return "기본"; }
