import {
    type InfiniteData,
    useInfiniteQuery,
    useMutation,
    useQuery,
    useQueryClient,
} from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";

const UNREAD_REFRESH_MS = 60_000;
const FEED_PAGE_SIZE = 20;
const NOTIFICATIONS_KEY = ["notifications"] as const;

export type NotificationType =
    "COMMENT_REPLY" | "CONTENT_REMOVED" | "PROBLEM_PUBLISHED" | "RATING_CHANGED" | "ANNOUNCEMENT";

export interface AppNotification {
    id: string;
    type: NotificationType;
    title: string;
    link: string | null;
    isRead: boolean;
    createdAt: string;
}

export interface NotificationFeed {
    items: AppNotification[];
    nextCursor: string | null;
}

export const notificationApi = {
    feed: (cursor: string | null, size: number, signal?: AbortSignal) =>
        apiClient.get<NotificationFeed>("/api/notifications", {
            params: { cursor: cursor ?? undefined, size },
            signal,
        }),
    unreadCount: (signal?: AbortSignal) =>
        apiClient.get<{ count: number }>("/api/notifications/unread-count", { signal }),
    markRead: (notificationId: string) =>
        apiClient.post<void>(`/api/notifications/${encodeURIComponent(notificationId)}/read`),
    markAllRead: () => apiClient.post<void>("/api/notifications/read-all"),
    preferences: (signal?: AbortSignal) =>
        apiClient.get<NotificationPreferences>("/api/notifications/preferences", { signal }),
    updatePreferences: (preferences: NotificationPreferences) =>
        apiClient.put<NotificationPreferences>("/api/notifications/preferences", preferences),
};

export interface NotificationPreferences {
    isEmailEnabled: boolean;
}

export function useNotificationPreferences(isEnabled: boolean) {
    const queryClient = useQueryClient();
    const key = [...NOTIFICATIONS_KEY, "preferences"];
    const query = useQuery({
        queryKey: key,
        queryFn: ({ signal }) => notificationApi.preferences(signal),
        enabled: isEnabled,
        retry: false,
    });
    const update = useMutation({
        mutationFn: notificationApi.updatePreferences,
        onSuccess: (saved) => queryClient.setQueryData(key, saved),
    });
    return { query, update };
}

export function useUnreadNotificationCount(isEnabled: boolean) {
    return useQuery({
        queryKey: [...NOTIFICATIONS_KEY, "unread-count"],
        queryFn: ({ signal }) => notificationApi.unreadCount(signal),
        enabled: isEnabled,
        refetchInterval: UNREAD_REFRESH_MS,
        retry: false,
    });
}

export function useNotificationFeed(isEnabled: boolean, size = FEED_PAGE_SIZE) {
    return useInfiniteQuery({
        queryKey: [...NOTIFICATIONS_KEY, "feed", size],
        queryFn: ({ pageParam, signal }) => notificationApi.feed(pageParam, size, signal),
        initialPageParam: null as string | null,
        getNextPageParam: (lastPage) => lastPage.nextCursor,
        enabled: isEnabled,
        retry: false,
    });
}

export function flattenFeed(data: InfiniteData<NotificationFeed> | undefined): AppNotification[] {
    return data?.pages.flatMap((page) => page.items) ?? [];
}

export function useMarkNotificationsRead() {
    const queryClient = useQueryClient();
    const refresh = () => queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_KEY });

    return {
        markRead: useMutation({ mutationFn: notificationApi.markRead, onSuccess: refresh }),
        markAllRead: useMutation({ mutationFn: notificationApi.markAllRead, onSuccess: refresh }),
    };
}
