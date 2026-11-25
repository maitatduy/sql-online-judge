"use client";

import { Bell } from "lucide-react";
import Link from "next/link";
import { Popover } from "radix-ui";
import { useState } from "react";

import { NotificationList } from "@/components/shared/NotificationList";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import {
    flattenFeed,
    useMarkNotificationsRead,
    useNotificationFeed,
    useUnreadNotificationCount,
} from "@/lib/notifications";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { useNow } from "@/lib/useNow";

const PREVIEW_SIZE = 8;
const MAX_BADGE = 99;

export function NotificationBell() {
    const now = useNow();
    const [isOpen, setIsOpen] = useState(false);
    const { data: currentUser } = useCurrentUser();
    const unread = useUnreadNotificationCount(Boolean(currentUser));
    const feed = useNotificationFeed(isOpen, PREVIEW_SIZE);
    const { markRead, markAllRead } = useMarkNotificationsRead();

    if (!currentUser) {
        return null;
    }

    const count = unread.data?.count ?? 0;
    const items = flattenFeed(feed.data).slice(0, PREVIEW_SIZE);
    const label = count > 0 ? `Thông báo, ${formatNumber(count)} chưa đọc` : "Thông báo";

    return (
        <Popover.Root open={isOpen} onOpenChange={setIsOpen}>
            <Popover.Trigger asChild>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label={label}
                    className="relative size-11"
                >
                    <Bell aria-hidden="true" className="size-5.5" />
                    {count > 0 ? (
                        <span
                            aria-hidden="true"
                            className="absolute top-1.5 right-1.5 min-w-4 rounded-full bg-brand px-1 text-[10px] leading-4 font-bold text-white"
                        >
                            {count > MAX_BADGE ? `${MAX_BADGE}+` : count}
                        </span>
                    ) : null}
                </Button>
            </Popover.Trigger>
            <Popover.Portal>
                <Popover.Content
                    align="end"
                    sideOffset={8}
                    aria-label="Thông báo mới"
                    className="z-50 w-[min(380px,calc(100vw-32px))] overflow-hidden rounded-lg border bg-card shadow-lg"
                >
                    <div className="flex items-center justify-between border-b px-4 py-3">
                        <span className="font-bold">Thông báo</span>
                        {count > 0 ? (
                            <button
                                type="button"
                                onClick={() => markAllRead.mutate()}
                                className="text-sm font-semibold text-brand hover:underline"
                            >
                                Đánh dấu đã đọc
                            </button>
                        ) : null}
                    </div>
                    <div className="max-h-96 overflow-y-auto">
                        {feed.isPending ? <Skeleton className="m-4 h-24" /> : null}
                        {feed.data && items.length === 0 ? (
                            <p className="px-4 py-8 text-center text-[15px] text-muted-foreground">
                                Chưa có thông báo nào.
                            </p>
                        ) : null}
                        {items.length > 0 ? (
                            <NotificationList
                                items={items}
                                now={now}
                                onOpen={(notification) => {
                                    if (!notification.isRead) {
                                        markRead.mutate(notification.id);
                                    }
                                    setIsOpen(false);
                                }}
                            />
                        ) : null}
                    </div>
                    <Link
                        href={ROUTES.notifications}
                        onClick={() => setIsOpen(false)}
                        className="block border-t px-4 py-2.5 text-center text-sm font-semibold text-brand hover:bg-surface-2"
                    >
                        Xem tất cả
                    </Link>
                </Popover.Content>
            </Popover.Portal>
        </Popover.Root>
    );
}
