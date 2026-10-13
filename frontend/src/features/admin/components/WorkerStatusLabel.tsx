import { cn } from "@/lib/utils";

import type { WorkerStatus } from "../types";

interface StatusAppearance {
    label: string;
    textClassName: string;
    dotClassName: string;
}

const STATUS_APPEARANCES: Record<WorkerStatus, StatusAppearance> = {
    ONLINE: { label: "Online", textClassName: "text-status-ok", dotClassName: "bg-status-ok" },
    BUSY: { label: "Bận", textClassName: "text-status-busy", dotClassName: "bg-status-busy" },
    DISCONNECTED: {
        label: "Mất kết nối",
        textClassName: "text-status-down",
        dotClassName: "bg-status-down",
    },
    RESTARTING: {
        label: "Khởi động lại",
        textClassName: "text-status-busy",
        dotClassName: "bg-status-busy",
    },
};

interface WorkerStatusLabelProps {
    status: WorkerStatus;
}

export function WorkerStatusLabel({ status }: WorkerStatusLabelProps) {
    const { label, textClassName, dotClassName } = STATUS_APPEARANCES[status];

    return (
        <span className={cn("inline-flex items-center gap-1.5 font-semibold", textClassName)}>
            <span aria-hidden="true" className={cn("size-2 shrink-0 rounded-full", dotClassName)} />
            {label}
        </span>
    );
}
