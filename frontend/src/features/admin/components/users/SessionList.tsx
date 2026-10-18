import { TriangleAlert } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import type { UserSession } from "@/features/admin/users/types";
import { formatRelativeTime } from "@/lib/format";

export function sessionLabel({ browser, os, city }: UserSession): string {
    return [browser, os, city].filter(Boolean).join(" · ");
}

interface SessionListProps {
    sessions: UserSession[];
    onRevoke?: (session: UserSession) => void;
}

export function SessionList({ sessions, onRevoke }: SessionListProps) {
    const [mountedAt] = useState(() => new Date());

    return (
        <ul className="flex flex-col text-[13px]">
            {sessions.map((session) => (
                <li
                    key={session.id}
                    className="flex flex-wrap items-center justify-between gap-2 border-t py-2 first:border-t-0"
                >
                    <span className="min-w-0">{sessionLabel(session)}</span>
                    <span className="flex items-center gap-2 text-muted-foreground">
                        {session.isUnusualLocation ? (
                            <span className="inline-flex items-center gap-1 rounded-full bg-surface-2 px-2 py-0.5 text-xs font-semibold text-foreground">
                                <TriangleAlert aria-hidden="true" className="size-3" />
                                IP lạ
                            </span>
                        ) : null}
                        {session.isActiveNow
                            ? "Đang hoạt động"
                            : formatRelativeTime(session.lastActiveAt, mountedAt)}
                        {onRevoke ? (
                            <Button
                                type="button"
                                size="xs"
                                variant="ghost"
                                aria-label={`Đăng xuất phiên ${sessionLabel(session)}`}
                                onClick={() => onRevoke(session)}
                            >
                                Đăng xuất
                            </Button>
                        ) : null}
                    </span>
                </li>
            ))}
        </ul>
    );
}
