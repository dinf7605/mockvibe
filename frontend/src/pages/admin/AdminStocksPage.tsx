import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listStocks, toggleStock } from "../../api/admin";

export function AdminStocksPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const { data } = useQuery({ queryKey: ["admin-stocks", page], queryFn: () => listStocks(page, 30) });

  const toggle = useMutation({
    mutationFn: (ticker: string) => toggleStock(ticker),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin-stocks"] }),
  });

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={card}>
        <table style={table} className="tabular">
          <thead><tr style={thRow}>
            <th style={th}>티커</th><th style={th}>회사</th>
            <th style={th}>시장</th><th style={th}>섹터</th>
            <th style={th}>상태</th><th style={th}>액션</th>
          </tr></thead>
          <tbody>
            {data?.content.map((s) => (
              <tr key={s.ticker} style={tdRow}>
                <td style={td}><strong>{s.ticker}</strong></td>
                <td style={td}>{s.companyName}</td>
                <td style={td}>{s.market}</td>
                <td style={td}>{s.sector ?? "-"}</td>
                <td style={td} className={s.active ? "up" : "down"}>
                  {s.active ? "활성" : "비활성"}
                </td>
                <td style={td}>
                  <button style={btn}
                          onClick={() => toggle.mutate(s.ticker)}
                          disabled={toggle.isPending}>
                    {s.active ? "비활성화" : "활성화"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {data && data.totalPages > 1 && (
        <div style={pager}>
          <button onClick={() => setPage(p => Math.max(p-1, 0))} disabled={page===0} style={pageBtn}>이전</button>
          <span>{data.number + 1} / {data.totalPages} ({data.totalElements}개)</span>
          <button onClick={() => setPage(p => p+1)} disabled={page+1 >= data.totalPages} style={pageBtn}>다음</button>
        </div>
      )}
    </div>
  );
}

const card: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: 12 };
const table: React.CSSProperties = { width: "100%", borderCollapse: "collapse", fontSize: 13 };
const thRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const th: React.CSSProperties = { textAlign: "left" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 };
const tdRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const td: React.CSSProperties = { padding: "10px 8px" };
const btn: React.CSSProperties = { height: 28, padding: "0 12px", fontSize: 12, background: "var(--bg-elevated)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
const pager: React.CSSProperties = { display: "flex", gap: 12, alignItems: "center", justifyContent: "center" };
const pageBtn: React.CSSProperties = { ...btn, height: 32, padding: "0 14px" };
