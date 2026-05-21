import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getCacheStats, listCircuitBreakers, resetCircuitBreaker } from "../../api/admin";

export function AdminSystemPage() {
  const qc = useQueryClient();
  const cbs = useQuery({ queryKey: ["admin-cb"], queryFn: listCircuitBreakers, refetchInterval: 10_000 });
  const cache = useQuery({ queryKey: ["admin-cache"], queryFn: getCacheStats, refetchInterval: 10_000 });

  const reset = useMutation({
    mutationFn: (name: string) => resetCircuitBreaker(name),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin-cb"] }),
  });

  return (
    <div style={{ display: "grid", gap: 20 }}>
      <Card title="Circuit Breakers (외부 API 헬스)">
        <div style={cbGrid}>
          {cbs.data?.map((cb) => (
            <div key={cb.name} style={cbCard}>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <span style={dot(cb.state)} />
                <strong>{cb.name}</strong>
                <span style={stateBadge(cb.state)}>{cb.state}</span>
              </div>
              <div style={cbMetrics}>
                <span>실패율 {(cb.failureRate * 100).toFixed(1)}%</span>
                <span className="up">실패 {cb.failedCalls}</span>
                <span className="flat">성공 {cb.successCalls}</span>
              </div>
              <button style={btn} onClick={() => reset.mutate(cb.name)} disabled={reset.isPending}>
                Reset
              </button>
            </div>
          ))}
        </div>
      </Card>

      <Card title="PriceCache">
        <div style={{ fontSize: 22, fontWeight: 700 }} className="tabular">
          {cache.data?.priceCacheSize ?? "-"}
          <span style={{ fontSize: 12, color: "var(--text-secondary)", marginLeft: 6 }}>개 종목 캐싱 중</span>
        </div>
      </Card>
    </div>
  );
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return <section style={card}><div style={cardTitle}>{title}</div>{children}</section>;
}

function dot(state: string): React.CSSProperties {
  const c = state === "CLOSED" ? "var(--color-success)" : state === "OPEN" ? "var(--color-danger)" : "var(--color-warning)";
  return { width: 10, height: 10, borderRadius: 999, background: c, display: "inline-block" };
}
function stateBadge(state: string): React.CSSProperties {
  return { fontSize: 10, padding: "2px 8px", borderRadius: 999,
    background: state === "CLOSED" ? "rgba(22,163,74,0.15)" : "var(--color-up-bg)",
    color: state === "CLOSED" ? "var(--color-success)" : "var(--color-up)" };
}

const card: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: 20 };
const cardTitle: React.CSSProperties = { fontSize: 13, fontWeight: 700, color: "var(--text-secondary)", marginBottom: 12 };
const cbGrid: React.CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 12 };
const cbCard: React.CSSProperties = { padding: 14, background: "var(--bg-elevated)", borderRadius: "var(--radius-md)", display: "flex", flexDirection: "column", gap: 10 };
const cbMetrics: React.CSSProperties = { display: "flex", gap: 12, fontSize: 12, color: "var(--text-secondary)" };
const btn: React.CSSProperties = { height: 28, padding: "0 12px", fontSize: 12, background: "var(--bg-panel)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
