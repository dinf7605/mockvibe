interface Props {
  title: string;
  day: string;          // 예: "D18"
  description: string;
}

export function Placeholder({ title, day, description }: Props) {
  return (
    <section
      style={{
        background: "var(--bg-elevated)",
        border: "1px solid var(--border-subtle)",
        borderRadius: "var(--radius-lg)",
        padding: 32,
      }}
    >
      <div
        style={{
          display: "inline-block",
          padding: "2px 8px",
          background: "var(--color-up-bg)",
          color: "var(--color-up)",
          borderRadius: 999,
          fontSize: 11,
          fontWeight: 700,
          marginBottom: 12,
        }}
      >
        {day} 예정
      </div>
      <h1 style={{ margin: "4px 0 8px", fontSize: 22 }}>{title}</h1>
      <p style={{ color: "var(--text-secondary)", margin: 0 }}>{description}</p>
    </section>
  );
}
