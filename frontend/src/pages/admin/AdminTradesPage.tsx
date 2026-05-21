import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { listTrades } from "../../api/admin";
import { formatKrw } from "../../lib/format";

export function AdminTradesPage() {
  const [page, setPage] = useState(0);
  const { data } = useQuery({ queryKey: ["admin-trades", page], queryFn: () => listTrades(page, 30) });

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={card}>
        <table style={table} className="tabular">
          <thead><tr style={thRow}>
            <th style={th}>일시</th><th style={th}>사용자</th><th style={th}>종목</th>
            <th style={th}>구분</th><th style={thNum}>수량</th><th style={thNum}>금액(KRW)</th>
          </tr></thead>
          <tbody>
            {data?.content.map((o) => (
              <tr key={o.orderId} style={tdRow}>
                <td style={td}>{new Date(o.createdAt).toLocaleString("ko-KR")}</td>
                <td style={td}>{o.userId.slice(0,8)}...</td>
                <td style={td}>{o.ticker}</td>
                <td style={td} className={o.orderType === "BUY" ? "up" : "down"}>
                  {o.orderType === "BUY" ? "매수" : "매도"}·{o.orderMethod === "MARKET" ? "시장가" : "지정가"}
                </td>
                <td style={tdNum}>{o.quantity}</td>
                <td style={tdNum}>{formatKrw(o.totalAmountKrw)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      {data && data.totalPages > 1 && (
        <Pager page={data.number} totalPages={data.totalPages} onChange={setPage} />
      )}
    </div>
  );
}

export function Pager({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  return (
    <div style={pager}>
      <button onClick={() => onChange(Math.max(page-1, 0))} disabled={page===0} style={pageBtn}>이전</button>
      <span>{page + 1} / {totalPages}</span>
      <button onClick={() => onChange(page+1)} disabled={page+1 >= totalPages} style={pageBtn}>다음</button>
    </div>
  );
}

const card: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: 12 };
const table: React.CSSProperties = { width: "100%", borderCollapse: "collapse", fontSize: 13 };
const thRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const th: React.CSSProperties = { textAlign: "left" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 };
const thNum: React.CSSProperties = { ...th, textAlign: "right" as const };
const tdRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const td: React.CSSProperties = { padding: "10px 8px" };
const tdNum: React.CSSProperties = { padding: "10px 8px", textAlign: "right" as const };
const pager: React.CSSProperties = { display: "flex", gap: 12, alignItems: "center", justifyContent: "center" };
const pageBtn: React.CSSProperties = { height: 32, padding: "0 14px", fontSize: 13, background: "var(--bg-panel)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
