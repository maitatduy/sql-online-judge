import { Check, CircleAlert, LoaderCircle, type LucideIcon } from "lucide-react";

import { cn } from "@/lib/utils";

import type { UsernameAvailabilityStatus } from "../hooks/useUsernameAvailability";

interface UsernameAvailabilityProps {
    status: UsernameAvailabilityStatus;
}

type VisibleStatus = Exclude<UsernameAvailabilityStatus, "idle">;

const STATUS_CONTENT: Record<
    VisibleStatus,
    { label: string; icon: LucideIcon; className: string; iconClassName?: string }
> = {
    checking: {
        label: "Đang kiểm tra…",
        icon: LoaderCircle,
        className: "text-muted-foreground",
        iconClassName: "motion-safe:animate-spin",
    },
    available: { label: "Còn trống", icon: Check, className: "text-verdict-ac" },
    taken: { label: "Đã có người dùng", icon: CircleAlert, className: "text-verdict-wa" },
};

export function UsernameAvailability({ status }: UsernameAvailabilityProps) {
    const content = status === "idle" ? null : STATUS_CONTENT[status];

    return (
        <p
            role="status"
            className={cn(
                "flex items-center gap-1.5 text-xs",
                content && "mt-1.5",
                content?.className,
            )}
        >
            {content && (
                <>
                    <content.icon
                        aria-hidden="true"
                        className={cn("size-3.5", content.iconClassName)}
                    />
                    {content.label}
                </>
            )}
        </p>
    );
}
