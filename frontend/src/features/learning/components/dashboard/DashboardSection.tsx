import Link from "next/link";
import { useId } from "react";

import { cn } from "@/lib/utils";

interface DashboardSectionProps {
    title: string;
    action?: { label: string; href: string };
    aside?: React.ReactNode;
    className?: string;
    children: React.ReactNode;
}

export function DashboardSection({
    title,
    action,
    aside,
    className,
    children,
}: DashboardSectionProps) {
    const titleId = useId();

    return (
        <section
            aria-labelledby={titleId}
            className={cn("flex min-w-0 flex-col gap-4 rounded-lg border bg-card p-6", className)}
        >
            <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 id={titleId} className="text-lg font-bold">
                    {title}
                </h2>
                {action ? (
                    <Link href={action.href} className="text-sm font-medium text-brand">
                        {action.label}
                    </Link>
                ) : null}
                {aside}
            </div>
            {children}
        </section>
    );
}
