const STEPS = ["시작", "진단", "결과"];

export default function Stepper({ activeIndex }: { activeIndex: number }) {
  return (
    <div style={{ display: "flex", justifyContent: "center", gap: 36, padding: "22px 0 4px 0" }}>
      {STEPS.map((label, i) => (
        <div key={label} style={{ display: "flex", alignItems: "center" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <div
              style={{
                width: 24,
                height: 24,
                borderRadius: "50%",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: 12,
                fontWeight: 700,
                background: i < activeIndex ? "var(--primary-tint)" : i === activeIndex ? "var(--primary)" : "var(--border)",
                color: i < activeIndex ? "var(--primary)" : i === activeIndex ? "#fff" : "var(--text-faint)",
              }}
            >
              {i < activeIndex ? "✓" : i + 1}
            </div>
            <span
              style={{
                fontSize: 13,
                fontWeight: i === activeIndex ? 700 : 500,
                color: i === activeIndex ? "var(--primary)" : i < activeIndex ? "var(--text-muted)" : "var(--text-faint)",
              }}
            >
              {label}
            </span>
          </div>
          {i < STEPS.length - 1 && (
            <div
              style={{
                width: 50,
                height: 1.5,
                marginLeft: 12,
                background: i < activeIndex ? "var(--primary)" : "var(--border)",
              }}
            />
          )}
        </div>
      ))}
    </div>
  );
}
