import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { activateUser, adjustCash, issueStepUp, listUsers, suspendUser, type AdminUserView } from "../../api/admin";
import { formatKrw } from "../../lib/format";

export function AdminUsersPage() {
  const [page, setPage] = useState(0);
  const { data } = useQuery({ queryKey: ["admin-users", page], queryFn: () => listUsers(page, 20) });
  const [target, setTarget] = useState<AdminUserView | null>(null);

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={card}>
        <table style={table} className="tabular">
          <thead><tr style={thRow}>
            <th style={th}>사용자</th><th style={th}>이메일</th>
            <th style={th}>권한</th><th style={th}>상태</th>
            <th style={thNum}>최종 로그인</th><th style={th}>액션</th>
          </tr></thead>
          <tbody>
            {data?.content.map((u) => (
              <tr key={u.userId} style={tdRow}>
                <td style={td}><strong>{u.username}</strong><div style={sub}>{u.userId.slice(0,8)}...</div></td>
                <td style={td}>{u.email}</td>
                <td style={td}>{u.role}</td>
                <td style={td} className={u.status === "ACTIVE" ? "up" : "down"}>{u.status}</td>
                <td style={tdNum}>{u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString("ko-KR") : "-"}</td>
                <td style={td}>
                  <button style={btn} onClick={() => setTarget(u)}>관리</button>
                </td>
              </tr>
            ))}
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

      {target && <UserActionModal user={target} onClose={() => setTarget(null)} />}
    </div>
  );
}

function UserActionModal({ user, onClose }: { user: AdminUserView; onClose: () => void }) {
  const qc = useQueryClient();
  const [stepUpToken, setStepUpToken] = useState<string | null>(null);
  const [password, setPassword] = useState("");
  const [amount, setAmount] = useState("");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  const refresh = () => { qc.invalidateQueries({ queryKey: ["admin-users"] }); onClose(); };

  const suspend = useMutation({
    mutationFn: () => user.status === "ACTIVE" ? suspendUser(user.userId) : activateUser(user.userId),
    onSuccess: refresh,
    onError: (e) => setError(messageOf(e)),
  });

  const issueToken = useMutation({
    mutationFn: () => issueStepUp(password),
    onSuccess: (r) => { setStepUpToken(r.stepUpToken); setError(null); },
    onError: (e) => setError(messageOf(e)),
  });

  const cash = useMutation({
    mutationFn: () => adjustCash(user.userId, Number(amount), reason, stepUpToken!),
    onSuccess: refresh,
    onError: (e) => setError(messageOf(e)),
  });

  return (
    <div style={overlay} onClick={onClose}>
      <div style={modal} onClick={(e) => e.stopPropagation()}>
        <div style={{ fontSize: 16, fontWeight: 700, marginBottom: 4 }}>{user.username}</div>
        <div style={sub}>{user.email} · {user.role} · {user.status}</div>

        <div style={section}>
          <div style={sectionTitle}>계정 상태</div>
          <button style={btnFull} onClick={() => suspend.mutate()}>
            {user.status === "ACTIVE" ? "계정 정지" : "정지 해제"}
          </button>
        </div>

        <div style={section}>
          <div style={sectionTitle}>시드머니 조정 <span style={dangerTag}>step-up</span></div>
          {!stepUpToken ? (
            <div style={col}>
              <input type="password" placeholder="본인 비밀번호 재입력"
                value={password} onChange={(e) => setPassword(e.target.value)} style={input} />
              <button style={btnPrimary} onClick={() => issueToken.mutate()} disabled={issueToken.isPending}>
                {issueToken.isPending ? "확인 중..." : "재인증"}
              </button>
            </div>
          ) : (
            <div style={col}>
              <input type="number" placeholder="조정 금액 (음수=차감)"
                value={amount} onChange={(e) => setAmount(e.target.value)} style={input} />
              <input placeholder="사유" value={reason} onChange={(e) => setReason(e.target.value)} style={input} />
              <button style={btnPrimary} onClick={() => cash.mutate()} disabled={cash.isPending || !amount}>
                조정 실행 {amount && `(${formatKrw(Number(amount))})`}
              </button>
            </div>
          )}
        </div>

        {error && <div style={errStyle}>{error}</div>}
        <button style={btnClose} onClick={onClose}>닫기</button>
      </div>
    </div>
  );
}

interface ApiErrorBody { message?: string }
interface HasResponse { response?: { data?: ApiErrorBody } }
function messageOf(e: unknown): string {
  const o = e as HasResponse | Error;
  return (o as HasResponse).response?.data?.message ?? (o as Error).message ?? "요청 실패";
}

const card: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: 12 };
const table: React.CSSProperties = { width: "100%", borderCollapse: "collapse", fontSize: 13 };
const thRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const th: React.CSSProperties = { textAlign: "left" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 };
const thNum: React.CSSProperties = { ...th, textAlign: "right" as const };
const tdRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const td: React.CSSProperties = { padding: "10px 8px" };
const tdNum: React.CSSProperties = { padding: "10px 8px", textAlign: "right" as const };
const sub: React.CSSProperties = { fontSize: 11, color: "var(--text-tertiary)" };
const btn: React.CSSProperties = { height: 28, padding: "0 12px", fontSize: 12, background: "var(--bg-elevated)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
const pager: React.CSSProperties = { display: "flex", gap: 12, alignItems: "center", justifyContent: "center" };
const pageBtn: React.CSSProperties = { ...btn, height: 32, padding: "0 14px" };
const overlay: React.CSSProperties = { position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)", display: "grid", placeItems: "center", zIndex: 100 };
const modal: React.CSSProperties = { background: "var(--bg-panel)", borderRadius: "var(--radius-lg)", padding: 24, width: 400, maxWidth: "calc(100% - 32px)", boxShadow: "var(--shadow-lg)" };
const section: React.CSSProperties = { marginTop: 20, paddingTop: 16, borderTop: "1px solid var(--border-subtle)" };
const sectionTitle: React.CSSProperties = { fontSize: 13, fontWeight: 700, marginBottom: 10, display: "flex", alignItems: "center", gap: 8 };
const dangerTag: React.CSSProperties = { fontSize: 10, padding: "2px 8px", background: "var(--color-up-bg)", color: "var(--color-up)", borderRadius: 999 };
const col: React.CSSProperties = { display: "flex", flexDirection: "column", gap: 8 };
const input: React.CSSProperties = { height: 36, padding: "0 10px", fontSize: 13, background: "var(--bg-base)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
const btnPrimary: React.CSSProperties = { height: 36, background: "var(--color-primary)", color: "#fff", border: "none", borderRadius: "var(--radius-sm)", fontSize: 13, fontWeight: 600 };
const btnFull: React.CSSProperties = { height: 36, padding: "0 14px", background: "var(--bg-elevated)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)", fontSize: 13 };
const btnClose: React.CSSProperties = { marginTop: 20, height: 36, width: "100%", background: "transparent", color: "var(--text-secondary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)", fontSize: 13 };
const errStyle: React.CSSProperties = { marginTop: 12, padding: "8px 12px", fontSize: 12, color: "var(--color-danger)", background: "var(--color-up-bg)", borderRadius: "var(--radius-sm)" };
