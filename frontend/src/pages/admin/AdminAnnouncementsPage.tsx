import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createAnnouncement, deleteAnnouncement, listAnnouncements, toggleAnnouncement,
  updateAnnouncement, type AnnouncementUpsert, type AnnouncementView,
} from "../../api/admin";

export function AdminAnnouncementsPage() {
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ["admin-announcements"], queryFn: () => listAnnouncements(0, 50) });
  const [editing, setEditing] = useState<AnnouncementView | null>(null);
  const [creating, setCreating] = useState(false);

  const toggle = useMutation({
    mutationFn: (id: number) => toggleAnnouncement(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin-announcements"] }),
  });
  const remove = useMutation({
    mutationFn: (id: number) => deleteAnnouncement(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin-announcements"] }),
  });

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <div style={{ display: "flex", justifyContent: "flex-end" }}>
        <button style={btnPrimary} onClick={() => setCreating(true)}>+ 새 공지</button>
      </div>

      <section style={card}>
        <table style={table} className="tabular">
          <thead><tr style={thRow}>
            <th style={th}>제목</th><th style={th}>레벨</th>
            <th style={th}>상태</th><th style={th}>기간</th><th style={th}>액션</th>
          </tr></thead>
          <tbody>
            {data?.content.map((a) => (
              <tr key={a.announcementId} style={tdRow}>
                <td style={td}>{a.title}</td>
                <td style={td}><LevelBadge level={a.level} /></td>
                <td style={td} className={a.active ? "up" : "down"}>{a.active ? "활성" : "비활성"}</td>
                <td style={td}>{a.startsAt ? new Date(a.startsAt).toLocaleDateString("ko-KR") : "-"} ~ {a.endsAt ? new Date(a.endsAt).toLocaleDateString("ko-KR") : "-"}</td>
                <td style={td}>
                  <button style={btn} onClick={() => setEditing(a)}>수정</button>
                  <button style={btn} onClick={() => toggle.mutate(a.announcementId)}>{a.active ? "비활성" : "활성"}</button>
                  <button style={{...btn, color: "var(--color-danger)"}} onClick={() => { if (confirm("삭제하시겠습니까?")) remove.mutate(a.announcementId); }}>삭제</button>
                </td>
              </tr>
            ))}
            {(!data || data.content.length === 0) && (
              <tr><td colSpan={5} style={{ padding: 24, textAlign: "center", color: "var(--text-tertiary)" }}>등록된 공지가 없습니다.</td></tr>
            )}
          </tbody>
        </table>
      </section>

      {(creating || editing) && (
        <UpsertModal
          initial={editing}
          onClose={() => { setCreating(false); setEditing(null); }}
          onSaved={() => qc.invalidateQueries({ queryKey: ["admin-announcements"] })}
        />
      )}
    </div>
  );
}

function LevelBadge({ level }: { level: "INFO" | "WARNING" | "CRITICAL" }) {
  const c = level === "CRITICAL" ? "var(--color-danger)" : level === "WARNING" ? "var(--color-warning)" : "var(--text-secondary)";
  return <span style={{ fontSize: 11, padding: "2px 8px", borderRadius: 999, background: "var(--bg-elevated)", color: c }}>{level}</span>;
}

function UpsertModal({ initial, onClose, onSaved }: {
  initial: AnnouncementView | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form, setForm] = useState<AnnouncementUpsert>({
    title: initial?.title ?? "",
    content: initial?.content ?? "",
    level: initial?.level ?? "INFO",
    startsAt: initial?.startsAt ?? null,
    endsAt: initial?.endsAt ?? null,
  });
  const save = useMutation({
    mutationFn: () => initial
      ? updateAnnouncement(initial.announcementId, form)
      : createAnnouncement(form),
    onSuccess: () => { onSaved(); onClose(); },
  });

  return (
    <div style={overlay} onClick={onClose}>
      <div style={modal} onClick={(e) => e.stopPropagation()}>
        <div style={{ fontSize: 16, fontWeight: 700, marginBottom: 12 }}>
          {initial ? "공지 수정" : "새 공지"}
        </div>
        <input placeholder="제목" value={form.title} onChange={(e) => setForm({...form, title: e.target.value})} style={input} />
        <textarea placeholder="내용" rows={5} value={form.content} onChange={(e) => setForm({...form, content: e.target.value})} style={{...input, height: "auto", padding: 10, fontFamily: "inherit"}} />
        <select value={form.level} onChange={(e) => setForm({...form, level: e.target.value as AnnouncementUpsert["level"]})} style={input}>
          <option value="INFO">INFO</option>
          <option value="WARNING">WARNING</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>
        <div style={{ display: "flex", gap: 8 }}>
          <button style={{...btnPrimary, flex: 1}} onClick={() => save.mutate()} disabled={save.isPending || !form.title || !form.content}>
            {save.isPending ? "저장 중..." : "저장"}
          </button>
          <button style={{...btn, flex: 1, height: 36}} onClick={onClose}>취소</button>
        </div>
      </div>
    </div>
  );
}

const card: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: 12 };
const table: React.CSSProperties = { width: "100%", borderCollapse: "collapse", fontSize: 13 };
const thRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const th: React.CSSProperties = { textAlign: "left" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 };
const tdRow: React.CSSProperties = { borderBottom: "1px solid var(--border-subtle)" };
const td: React.CSSProperties = { padding: "10px 8px" };
const btn: React.CSSProperties = { height: 28, padding: "0 10px", fontSize: 12, marginRight: 4, background: "var(--bg-elevated)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
const btnPrimary: React.CSSProperties = { height: 36, padding: "0 16px", background: "var(--color-primary)", color: "#fff", border: "none", borderRadius: "var(--radius-sm)", fontSize: 13, fontWeight: 600 };
const overlay: React.CSSProperties = { position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)", display: "grid", placeItems: "center", zIndex: 100 };
const modal: React.CSSProperties = { background: "var(--bg-panel)", borderRadius: "var(--radius-lg)", padding: 24, width: 480, maxWidth: "calc(100% - 32px)", display: "flex", flexDirection: "column", gap: 10 };
const input: React.CSSProperties = { height: 36, padding: "0 10px", fontSize: 13, background: "var(--bg-base)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)" };
