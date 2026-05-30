import { api } from "./client";

export type AiReportType = "TRADE_COMMENT" | "WEEKLY" | "INSTANT";

export interface AiReport {
  reportId: number;
  reportType: AiReportType;
  content: string;
  tokenUsed: number | null;
  createdAt: string;
}

interface AiReportList {
  items: AiReport[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** 최근 AI 리포트 목록 (기본 1건). */
export async function getAiReports(size = 1, type?: AiReportType): Promise<AiReport[]> {
  const res = await api.get<AiReportList>("/ai/reports", { params: { size, type } });
  return res.data.items;
}

/** 즉시 포트폴리오 분석 (일일 한도 적용). */
export async function analyzeNow(): Promise<AiReport> {
  const res = await api.post<AiReport>("/ai/analyze");
  return res.data;
}

export const AI_TYPE_LABEL: Record<AiReportType, string> = {
  TRADE_COMMENT: "매매 코멘트",
  WEEKLY: "주간 회고",
  INSTANT: "즉시 분석",
};
