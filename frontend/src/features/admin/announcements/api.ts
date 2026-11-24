import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

export interface Announcement {
    id: string;
    title: string;
    link: string | null;
    recipientCount: number;
    isDelivered: boolean;
    createdAt: string;
}

export interface NewAnnouncement {
    title: string;
    link?: string;
}

export const announcementApi = {
    list: (signal?: AbortSignal) =>
        apiClient.get<Page<Announcement>>("/api/admin/announcements", {
            params: { page: 1, size: 20 },
            signal,
        }),
    send: (announcement: NewAnnouncement) =>
        apiClient.post<Announcement>("/api/admin/announcements", announcement),
};
