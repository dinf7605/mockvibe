import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { listAuditLogs, type AuditLogView } from "../../api/admin";

export function AdminAuditPage() {
  const [page, setPage] = useState(0);
  const [targetType, setTargetType] = useState("");
  const [targetId, setTargetId] = useState("");
  const { data } = useQuery({
    queryKey: ["admin-audit", page, targetType, targetId],
    queryFn: () => listAuditLogs(page, 50, targetType || undefined, targetId || undefined),
  });
  const [selected, setSelected] = useState<AuditLogView | null>(null);

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={{ display: "flex", gap: 8 }}>
        <input placeholder="targetType (USER/STOCK/ANNOUNCEMENT)" value={targetType} onChange={(e) => { setTargetType(e.target.value); setPage(0); }} style={input} />
        <input placeholder="targetId" value={targetId} onChange={(e) => { setTargetId(e.target.value); setPage(0); }} style={input} />
      </section>

      <section style={card}>
        <table style={table} className="tabular">
          <thead><tr style={thRow}>
            <th style={th}>일시</th><th style={th}>관리자</th><th style={th}>액션</th>
            <th style={th}>대상</th><th style={th}>IP</th><th style={th}>상세</th>
          </tr></thead>
          <tbody>
            {data?.content.map((a) => (
              <tr key={a.auditId} style={tdRow}>
                <td style={td}>{new Date(a.createdAt).toLocaleString("ko-KR")}</td>
                <td style={td}>{a.adminUserId.slice(0,8)}...</td>
                <td style={td}><strong>{a.action}</strong></td>
                <td style={td}>{a.targetType}{a.targetId ? ` · ${a.targetId.slice(0, 20)}` : ""}</td>
                <td style={td}>{a.ipAddress ?? "-"}</td>
                <td style={td}><button style={btn} onClick={() => setSelected(a)}>diff</button></td>
              </tr>
            ))}
            {(!data || data.content.length === 0) && (
              <tr><td colSpan={6} style={{ padding: 24, textAlign: "center", color: "var(--text-tertiary)" }}>감사 로그가 없습니다.</td></tr>
            )}
          </tbody>
        </table>
      </section>

      {data && data.totalPages > 1 && (
        <div style={pager}>
          <button onClick={() => setPage(p => Math.max(p-1, 0))} disabled={page===0} style={pageBtn}>이전</button>
          <span>{data.number + 1} / {data.totalPages}</span>
          <button onClick={() => setPage(p => p+1)} disabled={page+1 >= data.totalPages} style={pageBtn}>다음</button>
        </div>
      )}

      {selected && <DiffModal entry={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}

function DiffModal({ entry, onClose }: { entry: AuditLogView; onClose: () => void }) {
  return (
    <div style={overlay} onClick={onClose}>
      <div style={modal} onClick={(e) => e.stopPropagation()}>
        <div style={{ fontSize: 16, fontWeight: 700 }}>{entry.action}</div>
        <div style={{ fontSize: 12, color: "var(--text-secondary)", marginBottom: 12 }}>
          {entry.targetType}{entry.targetId ? ` · ${entry.targetId}` : ""} · {new Date(entry.createdAt).toLocaleString("ko-KR")}
        </div>
        <Section title="Before" content={entry.beforeValue} />
        <Section title="After"  content={entry.afterValue} />
        <div style={{ fontSize: 11, color: "var(--text-tertiary)", marginTop: 12 }}>
          {entry.ipAddress ?? "?"} · {entry.userAgent ?? "?"}
        </div>
        <button style={{...btnClose}} onClick={onClose}>닫기</button>
      </div>
    </div>
  );
}

function Section({ title, content }: { title: string; content: string | null }) {
  return (
    <div style={{ marginTop: 10 }}>
      <div style={{ fontSize: 12, fontWeight: 700, color: "var(--text-secondary)", marginBottom: 6 }}>{title}</div>
      <pre style={pre}>{content ? prettify(content) : "(없음)"}</pre>
    </div>
  );
}
function prettify(s: string): string {
  try { return JSON.stringify(JSON.parse(s), null, 2); } catch { return s; }
}

const card: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: 12 };
const table: React.CSSProperties = { width: "100%", borderCollapse: "collapse", fontSize: 13 };
const thRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const th: React.CSSProperties = { textAlign: "left" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 };
const tdRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const td: React.CSSProperties = { padding: "10px 8px" };
const btn: React.CSSProperties = { height: 26, padding: "0 10px", fontSize: 11, background: "var(--bg-elevated)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
const pager: React.CSSProperties = { display: "flex", gap: 12, alignItems: "center", justifyContent: "center" };
const pageBtn: React.CSSProperties = { height: 32, padding: "0 14px", fontSize: 13, background: "var(--bg-panel)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
const input: React.CSSProperties = { flex: 1, height: 36, padding: "0 10px", fontSize: 13, background: "var(--bg-panel)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
const overlay: React.CSSProperties = { position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)", display: "grid", placeItems: "center", zIndex: 100 };
const modal: React.CSSProperties = { background: "var(--bg-panel)", borderRadius: "var(--radius-lg)", padding: 24, width: 560, maxWidth: "calc(100% - 32px)", maxHeight: "80vh", overflow: "auto" };
const pre: React.CSSProperties = { margin: 0, padding: 12, fontSize: 12, background: "var(--bg-elevated)", borderRadius: "var(--radius-sm)", overflowX: "auto" as const, whiteSpace: "pre-wrap", fontFamily: "var(--font-mono)" };
const btnClose: React.CSSProperties = { marginTop: 16, height: 36, width: "100%", background: "transparent", color: "var(--text-secondary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)", fontSize: 13 };
