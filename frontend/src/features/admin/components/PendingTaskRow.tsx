import { ChevronRight } from "lucide-react";
import Link from "next/link";

import { ADMIN_ROUTES } from "@/lib/admin-navigation";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { PendingTask, PendingTaskType } from "../types";

interface PendingTaskAppearance {
    label: string;
    href: string;
    countClassName: string;
}

const TASK_APPEARANCES: Record<PendingTaskType, PendingTaskAppearance> = {
    PROBLEM_REVIEW: {
        label: "Bài chờ duyệt",
        href: ADMIN_ROUTES.problemReviews,
        countClassName: "bg-count-review",
    },
    ABUSE_REPORT: {
        label: "Báo cáo vi phạm",
        href: ADMIN_ROUTES.reports,
        countClassName: "bg-count-report",
    },
    AI_FLAG: {
        label: "Hội thoại AI bị gắn cờ",
        href: ADMIN_ROUTES.ai,
        countClassName: "bg-count-ai-flag",
    },
    SUPPORT_MESSAGE: {
        label: "Tin nhắn hỗ trợ chưa trả lời",
        href: ADMIN_ROUTES.messages,
        countClassName: "bg-count-support",
    },
};

interface PendingTaskRowProps {
    task: PendingTask;
}

export function PendingTaskRow({ task }: PendingTaskRowProps) {
    const { label, href, countClassName } = TASK_APPEARANCES[task.type];

    return (
        <Link
            href={href}
            className="flex min-h-11 items-center gap-3 border-t px-4.5 py-3.5 hover:bg-surface"
        >
            <span className="min-w-0 flex-1">
                {label}
                <span className="mt-0.5 block text-xs text-muted-foreground">{task.detail}</span>
            </span>
            <span
                className={cn(
                    "inline-flex h-6 min-w-7 items-center justify-center rounded-full px-2 font-mono text-xs font-bold text-white",
                    countClassName,
                )}
            >
                <span>{formatNumber(task.count)}</span>
                <span className="sr-only"> việc</span>
            </span>
            <ChevronRight aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
        </Link>
    );
}
