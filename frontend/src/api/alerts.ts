import { api } from "./client";

export type AlertDirection = "ABOVE" | "BELOW";
export type AlertStatus = "ACTIVE" | "TRIGGERED" | "CANCELLED";

export interface Alert {
  alertId: number;
  ticker: string;
  direction: AlertDirection;
  targetPrice: number;
  status: AlertStatus;
  triggeredPrice: number | null;
  createdAt: string;
  triggeredAt: string | null;
}

export async function getAlerts(): Promise<Alert[]> {
  const res = await api.get<Alert[]>("/alerts");
  return res.data;
}

export async function createAlert(input: {
  ticker: string;
  direction: AlertDirection;
  targetPrice: number;
}): Promise<Alert> {
  const res = await api.post<Alert>("/alerts", input);
  return res.data;
}

export async function cancelAlert(alertId: number): Promise<void> {
  await api.delete(`/alerts/${alertId}`);
}

/** 알림 벨 배지용 — 미확인(TRIGGERED) 개수. */
export async function getTriggeredCount(): Promise<number> {
  const res = await api.get<{ count: number }>("/alerts/triggered-count");
  return res.data.count;
}
