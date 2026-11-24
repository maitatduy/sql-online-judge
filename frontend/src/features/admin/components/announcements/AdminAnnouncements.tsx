"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Megaphone } from "lucide-react";
import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { useNow } from "@/lib/useNow";

import { announcementApi } from "../../announcements/api";

const ANNOUNCEMENTS_KEY = ["admin", "announcements"] as const;
const MAX_TITLE_LENGTH = 300;
const REFRESH_WHILE_SENDING_MS = 3_000;

export function AdminAnnouncements() {
    const now = useNow();
    const showToast = useToast();
    const queryClient = useQueryClient();
    const [title, setTitle] = useState("");
    const [link, setLink] = useState("");
    const history = useQuery({
        queryKey: ANNOUNCEMENTS_KEY,
        queryFn: ({ signal }) => announcementApi.list(signal),
        refetchInterval: (query) =>
            query.state.data?.items.some((item) => !item.isDelivered)
                ? REFRESH_WHILE_SENDING_MS
                : false,
    });
    const send = useMutation({
        mutationFn: announcementApi.send,
        onSuccess: () => {
            setTitle("");
            setLink("");
            showToast("Đã bắt đầu gửi thông báo tới mọi người dùng.");
            return queryClient.invalidateQueries({ queryKey: ANNOUNCEMENTS_KEY });
        },
        onError: (error) => showToast(error.message, "error"),
    });

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-5 px-4 pt-6 pb-10 md:px-7">
            <h1 className="text-[24px] font-extrabold tracking-[-0.02em]">Thông báo hệ thống</h1>
            <form
                className="flex flex-col gap-3 rounded-lg border bg-card p-5"
                onSubmit={(event) => {
                    event.preventDefault();
                    send.mutate({ title: title.trim(), link: link.trim() || undefined });
                }}
            >
                <label htmlFor="announcement-title" className="font-semibold">
                    Nội dung
                </label>
                <Input
                    id="announcement-title"
                    maxLength={MAX_TITLE_LENGTH}
                    placeholder="Ví dụ: Máy chấm bảo trì từ 22 giờ đến 23 giờ tối nay"
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                    className="h-11 text-base"
                />
                <label htmlFor="announcement-link" className="font-semibold">
                    Đường dẫn (không bắt buộc)
                </label>
                <Input
                    id="announcement-link"
                    placeholder="/contests"
                    value={link}
                    onChange={(event) => setLink(event.target.value)}
                />
                <Button
                    type="submit"
                    disabled={!title.trim() || send.isPending}
                    className="self-end"
                >
                    <Megaphone aria-hidden="true" />
                    Gửi tới mọi người dùng
                </Button>
            </form>
            <section aria-labelledby="announcement-history" className="flex flex-col gap-3">
                <h2 id="announcement-history" className="text-lg font-bold">
                    Đã gửi
                </h2>
                {history.isPending ? <Skeleton className="h-32" /> : null}
                {!history.data && history.error ? (
                    <ErrorState
                        title="Chưa tải được lịch sử"
                        message={history.error.message}
                        traceId={
                            history.error instanceof ApiRequestError
                                ? history.error.traceId
                                : undefined
                        }
                        onRetry={() => void history.refetch()}
                    />
                ) : null}
                {history.data && history.data.items.length === 0 ? (
                    <EmptyState title="Chưa gửi thông báo nào." icon={<Megaphone />} />
                ) : null}
                {history.data && history.data.items.length > 0 ? (
                    <ul className="flex flex-col divide-y rounded-lg border bg-card">
                        {history.data.items.map((announcement) => (
                            <li key={announcement.id} className="flex flex-col gap-1 px-5 py-3">
                                <span className="text-[15px] font-semibold">
                                    {announcement.title}
                                </span>
                                <span className="text-[13px] text-muted-foreground">
                                    {[
                                        formatRelativeTime(announcement.createdAt, now),
                                        announcement.isDelivered
                                            ? `${formatNumber(announcement.recipientCount)} người nhận`
                                            : "Đang gửi…",
                                        announcement.link,
                                    ]
                                        .filter(Boolean)
                                        .join(" - ")}
                                </span>
                            </li>
                        ))}
                    </ul>
                ) : null}
            </section>
        </main>
    );
}
