import { useEffect, useMemo, useState } from "react";
import { api } from "../api/client";
import SimulationChart from "./SimulationChart";
import type { SimulationRunResponse, SimulationScenario } from "../api/types";

/**
 * 모의투자 패널.
 *
 * 실제 주문이 아니라 "개념 하나가 돈에 어떻게 작용하는지" 실험하는 도구다. 계산은 전부 서버에서 하고
 * 이 컴포넌트는 입력 폼 생성과 결과 표시만 한다. 입력 명세(min/max/기본값)도 서버가 내려주므로
 * 시나리오가 추가돼도 이 파일은 바뀌지 않는다.
 *
 * seed를 바꾸는 버튼을 따로 둔 이유는, 한 번의 결과를 법칙으로 읽는 것이 이 도구의 가장 큰 오용이기
 * 때문이다. 같은 가정에서 결과가 흩어지는 것을 보는 것 자체가 학습 내용이다.
 */
export default function SimulatorPanel({ slug }: { slug: string }) {
  const [scenarios, setScenarios] = useState<SimulationScenario[] | null>(null);
  const [activeId, setActiveId] = useState<string>("");
  const [values, setValues] = useState<Record<string, string>>({});
  const [seed, setSeed] = useState<number | undefined>(undefined);
  const [result, setResult] = useState<SimulationRunResponse | null>(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const active = useMemo(
    () => scenarios?.find(scenario => scenario.id === activeId) ?? scenarios?.[0] ?? null,
    [scenarios, activeId],
  );

  useEffect(() => {
    let alive = true;
    setScenarios(null);
    setResult(null);
    setError(null);
    api
      .getConceptSimulations(slug)
      .then(list => {
        if (!alive) return;
        setScenarios(list);
        setActiveId(list[0]?.id ?? "");
      })
      .catch(() => alive && setError("모의투자 시나리오를 불러오지 못했습니다."));
    return () => {
      alive = false;
    };
  }, [slug]);

  // 시나리오가 바뀌면 입력을 그 시나리오의 기본값으로 되돌린다.
  useEffect(() => {
    if (!active) return;
    setValues(Object.fromEntries(active.inputs.map(field => [field.key, field.defaultValue])));
    setSeed(undefined);
    setResult(null);
  }, [active]);

  async function run(nextSeed?: number) {
    if (!active) return;
    setRunning(true);
    setError(null);
    try {
      const response = await api.runSimulation(active.id, values, nextSeed);
      setResult(response);
      setSeed(response.seed);
    } catch {
      setError("모의투자를 실행하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setRunning(false);
    }
  }

  if (error && !scenarios)
    return (
      <div role="alert" className="panel-error">
        {error}
      </div>
    );
  if (!scenarios) return <p role="status">모의투자를 준비하는 중…</p>;
  if (scenarios.length === 0)
    return <p className="subtle">이 개념에는 아직 연결된 모의투자가 없습니다. 상위 개념에서 실험해 보세요.</p>;
  if (!active) return null;

  return (
    <section className="sim-panel" aria-label="모의투자">
      {scenarios.length > 1 && (
        <div className="sim-tabs" role="tablist">
          {scenarios.map(scenario => (
            <button
              key={scenario.id}
              role="tab"
              aria-selected={scenario.id === active.id}
              className="btn btn-secondary"
              onClick={() => setActiveId(scenario.id)}
            >
              {scenario.title}
            </button>
          ))}
        </div>
      )}

      <header className="sim-head">
        <span className="eyebrow">모의투자 · {active.id}</span>
        <h3>{active.question}</h3>
        <p className="subtle">{active.summary}</p>
        <p className="sim-mechanism">{active.mechanism}</p>
      </header>

      <form
        className="sim-form"
        onSubmit={event => {
          event.preventDefault();
          run(seed);
        }}
      >
        {active.inputs.map(field => (
          <div className="sim-field" key={field.key}>
            <label className="field-label" htmlFor={`${active.id}-${field.key}`}>
              {field.label}
              {field.unit ? ` (${field.unit})` : ""}
            </label>
            {field.type === "NUMBER" ? (
              <input
                id={`${active.id}-${field.key}`}
                className="field-input"
                type="number"
                inputMode="decimal"
                min={field.min ?? undefined}
                max={field.max ?? undefined}
                step={field.step ?? undefined}
                value={values[field.key] ?? field.defaultValue}
                onChange={event => setValues(prev => ({ ...prev, [field.key]: event.target.value }))}
              />
            ) : (
              <select
                id={`${active.id}-${field.key}`}
                className="field-input"
                value={values[field.key] ?? field.defaultValue}
                onChange={event => setValues(prev => ({ ...prev, [field.key]: event.target.value }))}
              >
                {field.choices.map(choice => (
                  <option key={choice.value} value={choice.value}>
                    {choice.label}
                  </option>
                ))}
              </select>
            )}
            <span className="sim-help">{field.help}</span>
          </div>
        ))}
        <div className="action-row sim-actions">
          <button className="btn btn-primary" type="submit" disabled={running}>
            {running ? "계산 중…" : result ? "다시 실행" : "모의투자 실행"}
          </button>
          {active.randomised && result && (
            <button
              className="btn btn-secondary"
              type="button"
              disabled={running}
              onClick={() => run(Math.floor(Math.random() * 1_000_000))}
            >
              다른 시장 상황 보기
            </button>
          )}
          {result && <span className="subtle">seed {result.seed}</span>}
        </div>
      </form>

      {error && (
        <div role="alert" className="panel-error">
          {error}
        </div>
      )}

      {result && (
        <div className="sim-result" aria-live="polite">
          <p className="sim-headline">{result.headline}</p>

          <div className="metric-grid">
            {result.metrics.map(metric => (
              <div className={`metric-card tone-${metric.tone.toLowerCase()}`} key={metric.label}>
                <span className="metric-label">{metric.label}</span>
                <strong className="metric-value">{metric.value}</strong>
                <span className="metric-note">{metric.note}</span>
              </div>
            ))}
          </div>

          {groupSeries(result).map((group, index) => (
            <SimulationChart key={index} series={group} />
          ))}

          {result.tables.map(table => (
            <div className="sim-table-wrap" key={table.title}>
              <h4>{table.title}</h4>
              <div className="table-scroll">
                <table className="sim-table">
                  <thead>
                    <tr>
                      {table.columns.map(column => (
                        <th key={column}>{column}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {table.rows.map(row => (
                      <tr key={row.label}>
                        <th scope="row">{row.label}</th>
                        {row.cells.map((cell, cellIndex) => (
                          <td key={cellIndex}>{cell}</td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <p className="subtle">{table.note}</p>
            </div>
          ))}

          <section className="sim-lessons">
            <h4>이 실행에서 확인된 것</h4>
            <ul>
              {result.lessons.map(lesson => (
                <li key={lesson}>{lesson}</li>
              ))}
            </ul>
          </section>

          <details className="sim-assumptions">
            <summary>계산에 사용한 가정 {result.assumptions.length}개</summary>
            <ul>
              {result.assumptions.map(assumption => (
                <li key={assumption}>{assumption}</li>
              ))}
            </ul>
          </details>
        </div>
      )}

      <p className="notice-line">{active.notice}</p>
    </section>
  );
}

/**
 * 단위가 같은 계열끼리 묶는다. 원(금액)과 p(지수)를 한 축에 그리면 한쪽이 직선처럼 눌려 보인다.
 * waterfall/bar는 계열 하나씩 따로 그린다.
 */
function groupSeries(result: SimulationRunResponse) {
  const groups: SimulationRunResponse["series"][] = [];
  for (const entry of result.series) {
    if (entry.kind !== "line") {
      groups.push([entry]);
      continue;
    }
    const existing = groups.find(group => group[0].kind === "line" && group[0].unit === entry.unit);
    if (existing) existing.push(entry);
    else groups.push([entry]);
  }
  return groups;
}
