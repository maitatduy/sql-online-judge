import type { LucideIcon } from "lucide-react";

import { cn } from "@/lib/utils";

interface AuthStatusCardProps {
    icon: LucideIcon;
    tone: "brand" | "destructive";
    title: string;
    description: React.ReactNode;
    isBusy?: boolean;
    children?: React.ReactNode;
}

export function AuthStatusCard({
    icon: Icon,
    tone,
    title,
    description,
    isBusy = false,
    children,
}: AuthStatusCardProps) {
    return (
        <div className="flex flex-col gap-5 rounded-xl border bg-card p-6 shadow-2xl sm:p-8">
            <span
                className={cn(
                    "inline-flex size-13 items-center justify-center rounded-xl",
                    tone === "brand"
                        ? "bg-brand-soft text-brand"
                        : "bg-destructive/12 text-destructive",
                )}
            >
                <Icon
                    aria-hidden="true"
                    data-testid="auth-status-icon"
                    className={cn("size-6.5", isBusy && "motion-safe:animate-spin")}
                />
            </span>
            <div>
                <h1 className="text-[26px] font-extrabold tracking-[-0.02em]">{title}</h1>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{description}</p>
            </div>
            {children}
        </div>
    );
}
