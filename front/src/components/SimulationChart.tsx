import type { SimulationSeries } from "../api/types";

/**
 * 결과 차트. 외부 차트 라이브러리를 쓰지 않고 SVG로 직접 그린다.
 *
 * 시나리오마다 계열 수와 스케일이 다르므로 모든 계열을 하나의 y축에 올리지 않는다.
 * 단위가 같은 계열만 묶고, 단위가 다르면 별도 차트로 분리해서 넘어온다(호출 측 책임).
 * 값 자체보다 '어느 쪽이 위에 있는가'를 읽는 그림이므로 눈금은 최소로 둔다.
 */
export default function SimulationChart({ series }: { series: SimulationSeries[] }) {
  if (series.length === 0) return null;
  const kind = series[0].kind;
  if (kind === "bar" || kind === "waterfall") return <BarChart series={series[0]} />;
  return <LineChart series={series} />;
}

const WIDTH = 720;
const HEIGHT = 240;
const PAD = { top: 16, right: 12, bottom: 28, left: 64 };
const COLORS = ["#2f55e8", "#d98324", "#3f9e6a", "#9450c8"];

function LineChart({ series }: { series: SimulationSeries[] }) {
  const all = series.flatMap(entry => entry.points.map(point => point.value));
  const max = Math.max(...all);
  const min = Math.min(...all);
  // 값이 모두 같으면 0으로 나누게 되므로 범위를 강제로 벌린다.
  const span = max - min || Math.abs(max) || 1;
  const low = min - span * 0.08;
  const high = max + span * 0.08;
  const count = Math.max(...series.map(entry => entry.points.length));

  const x = (index: number) => PAD.left + (index / Math.max(count - 1, 1)) * (WIDTH - PAD.left - PAD.right);
  const y = (value: number) =>
    PAD.top + (1 - (value - low) / (high - low)) * (HEIGHT - PAD.top - PAD.bottom);

  const ticks = [low, (low + high) / 2, high];

  return (
    <div className="chart-wrap">
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={series.map(entry => entry.name).join(", ") + " 추이 차트"}>
        {ticks.map(tick => (
          <g key={tick}>
            <line x1={PAD.left} x2={WIDTH - PAD.right} y1={y(tick)} y2={y(tick)} className="chart-grid" />
            <text x={PAD.left - 8} y={y(tick) + 4} className="chart-tick" textAnchor="end">
              {compact(tick)}
            </text>
          </g>
        ))}
        {series.map((entry, entryIndex) => (
          <polyline
            key={entry.name}
            fill="none"
            stroke={COLORS[entryIndex % COLORS.length]}
            strokeWidth={2}
            strokeLinejoin="round"
            points={entry.points.map((point, index) => `${x(index)},${y(point.value)}`).join(" ")}
          />
        ))}
        <text x={PAD.left} y={HEIGHT - 8} className="chart-tick">
          {series[0].points[0]?.label}
        </text>
        <text x={WIDTH - PAD.right} y={HEIGHT - 8} className="chart-tick" textAnchor="end">
          {series[0].points[series[0].points.length - 1]?.label}
        </text>
      </svg>
      <ul className="chart-legend">
        {series.map((entry, entryIndex) => (
          <li key={entry.name}>
            <span className="legend-swatch" style={{ background: COLORS[entryIndex % COLORS.length] }} />
            {entry.name}
          </li>
        ))}
      </ul>
    </div>
  );
}

function BarChart({ series }: { series: SimulationSeries }) {
  const values = series.points.map(point => point.value);
  const max = Math.max(...values, 0);
  const min = Math.min(...values, 0);
  const span = max - min || Math.abs(max) || 1;
  const barWidth = (WIDTH - PAD.left - PAD.right) / series.points.length;
  const zeroY = PAD.top + (max / span) * (HEIGHT - PAD.top - PAD.bottom);

  return (
    <div className="chart-wrap">
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={`${series.name} 막대 차트`}>
        <line x1={PAD.left} x2={WIDTH - PAD.right} y1={zeroY} y2={zeroY} className="chart-grid" />
        {series.points.map((point, index) => {
          const height = (Math.abs(point.value) / span) * (HEIGHT - PAD.top - PAD.bottom);
          const top = point.value >= 0 ? zeroY - height : zeroY;
          return (
            <g key={point.label}>
              <rect
                x={PAD.left + index * barWidth + barWidth * 0.18}
                y={top}
                width={barWidth * 0.64}
                height={Math.max(height, 1)}
                rx={3}
                fill={COLORS[0]}
                opacity={0.55 + 0.45 * (index / Math.max(series.points.length - 1, 1))}
              />
              <text
                x={PAD.left + index * barWidth + barWidth / 2}
                y={HEIGHT - 8}
                className="chart-tick"
                textAnchor="middle"
              >
                {point.label}
              </text>
              <text
                x={PAD.left + index * barWidth + barWidth / 2}
                y={top - 5}
                className="chart-value"
                textAnchor="middle"
              >
                {compact(point.value)}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

/** 축 라벨용 축약. 억/만 단위로 줄여 4자리 이상 숫자가 겹치지 않게 한다. */
function compact(value: number) {
  const abs = Math.abs(value);
  if (abs >= 100_000_000) return `${(value / 100_000_000).toFixed(1)}억`;
  if (abs >= 10_000) return `${Math.round(value / 10_000).toLocaleString()}만`;
  if (abs >= 1_000) return value.toLocaleString(undefined, { maximumFractionDigits: 0 });
  return value.toLocaleString(undefined, { maximumFractionDigits: 2 });
}
