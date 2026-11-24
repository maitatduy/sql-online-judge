"use client";

import { Bell } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { NotificationList } from "@/components/shared/NotificationList";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import {
    flattenFeed,
    useMarkNotificationsRead,
    useNotificationFeed,
    useUnreadNotificationCount,
} from "@/lib/notifications";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { useNow } from "@/lib/useNow";

export function NotificationsPage() {
    const pathname = usePathname();
    const now = useNow();
    const { data: currentUser, isPending: isUserPending } = useCurrentUser();
    const isSignedIn = Boolean(currentUser);
    const feed = useNotificationFeed(isSignedIn);
    const unread = useUnreadNotificationCount(isSignedIn);
    const { markRead, markAllRead } = useMarkNotificationsRead();
    const items = flattenFeed(feed.data);

    if (isUserPending) {
        return <Skeleton className="mx-auto mt-6 h-64 max-w-3xl" />;
    }

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-3xl flex-col gap-4 px-4 pt-6 pb-16"
        >
            <div className="flex flex-wrap items-center justify-between gap-3">
                <h1 className="text-[28px] font-extrabold">Thông báo</h1>
                {(unread.data?.count ?? 0) > 0 ? (
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={markAllRead.isPending}
                        onClick={() => markAllRead.mutate()}
                    >
                        Đánh dấu tất cả đã đọc
                    </Button>
                ) : null}
            </div>
            {!isSignedIn ? (
                <EmptyState
                    title="Đăng nhập để xem thông báo"
                    action={
                        <Button asChild size="sm">
                            <Link href={ROUTES.loginWithNext(pathname)}>Đăng nhập</Link>
                        </Button>
                    }
                />
            ) : null}
            {isSignedIn && feed.isPending ? <Skeleton className="h-64" /> : null}
            {!feed.data && feed.error ? (
                <ErrorState
                    title="Chưa tải được thông báo"
                    message={feed.error.message}
                    traceId={feed.error instanceof ApiRequestError ? feed.error.traceId : undefined}
                    onRetry={() => void feed.refetch()}
                />
            ) : null}
            {feed.data && items.length === 0 ? (
                <EmptyState title="Chưa có thông báo nào." icon={<Bell />} />
            ) : null}
            {items.length > 0 ? (
                <div className="overflow-hidden rounded-lg border bg-card">
                    <NotificationList
                        items={items}
                        now={now}
                        onOpen={(notification) => {
                            if (!notification.isRead) {
                                markRead.mutate(notification.id);
                            }
                        }}
                    />
                </div>
            ) : null}
            {feed.hasNextPage ? (
                <Button
                    type="button"
                    variant="outline"
                    disabled={feed.isFetchingNextPage}
                    onClick={() => void feed.fetchNextPage()}
                    className="self-center"
                >
                    Xem thêm
                </Button>
            ) : null}
        </main>
    );
}
