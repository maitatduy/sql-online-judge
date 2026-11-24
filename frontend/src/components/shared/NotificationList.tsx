"use client";

import { Award, CheckCircle2, MessageSquare, ShieldAlert } from "lucide-react";
import Link from "next/link";

import { formatRelativeTime } from "@/lib/format";
import type { AppNotification, NotificationType } from "@/lib/notifications";
import { cn } from "@/lib/utils";

const ICONS: Record<NotificationType, typeof MessageSquare> = {
    COMMENT_REPLY: MessageSquare,
    CONTENT_REMOVED: ShieldAlert,
    PROBLEM_PUBLISHED: CheckCircle2,
    RATING_CHANGED: Award,
};

interface NotificationListProps {
    items: AppNotification[];
    now: Date;
    onOpen: (notification: AppNotification) => void;
}

export function NotificationList({ items, now, onOpen }: NotificationListProps) {
    return (
        <ul aria-label="Thông báo" className="flex flex-col">
            {items.map((notification) => {
                const Icon = ICONS[notification.type];
                const body = (
                    <>
                        <Icon
                            aria-hidden="true"
                            className="mt-0.5 size-5 shrink-0 text-muted-foreground"
                        />
                        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                            <span
                                className={cn(
                                    "text-[15px]",
                                    !notification.isRead && "font-semibold",
                                )}
                            >
                                {notification.title}
                            </span>
                            <span className="text-[13px] text-muted-foreground">
                                {formatRelativeTime(notification.createdAt, now)}
                            </span>
                        </span>
                        {notification.isRead ? null : (
                            <span className="mt-2 size-2 shrink-0 rounded-full bg-brand">
                                <span className="sr-only">Chưa đọc</span>
                            </span>
                        )}
                    </>
                );
                const rowClass = cn(
                    "flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-surface-2",
                    !notification.isRead && "bg-brand-soft/60",
                );
                return (
                    <li key={notification.id} className="border-b last:border-b-0">
                        {notification.link ? (
                            <Link
                                href={notification.link}
                                onClick={() => onOpen(notification)}
                                className={rowClass}
                            >
                                {body}
                            </Link>
                        ) : (
                            <button
                                type="button"
                                onClick={() => onOpen(notification)}
                                className={rowClass}
                            >
                                {body}
                            </button>
                        )}
                    </li>
                );
            })}
        </ul>
    );
}
