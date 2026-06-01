import { api } from "./client";

export type NotificationType = "PRICE_ALERT" | "LIMIT_FILL" | "AI_COMMENT";

export interface AppNotification {
  notificationId: number;
  type: NotificationType;
  title: string;
  body: string | null;
  link: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  content: AppNotification[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export async function getNotifications(page = 0, size = 30): Promise<NotificationPage> {
  const res = await api.get<NotificationPage>("/notifications", { params: { page, size } });
  return res.data;
}

export async function getUnreadCount(): Promise<number> {
  const res = await api.get<{ count: number }>("/notifications/unread-count");
  return res.data.count;
}

export async function markNotificationRead(id: number): Promise<void> {
  await api.post(`/notifications/${id}/read`);
}

export async function markAllNotificationsRead(): Promise<void> {
  await api.post("/notifications/read-all");
}

export const NOTIF_ICON: Record<NotificationType, string> = {
  PRICE_ALERT: "🔔",
  LIMIT_FILL: "✅",
  AI_COMMENT: "🤖",
};
