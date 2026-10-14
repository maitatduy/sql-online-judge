import { useId } from "react";

import { cn } from "@/lib/utils";

interface PageCardProps {
    title: string;
    action?: React.ReactNode;
    className?: string;
    children: React.ReactNode;
}

export function PageCard({ title, action, className, children }: PageCardProps) {
    const titleId = useId();

    return (
        <section
            aria-labelledby={titleId}
            className={cn("rounded-lg border bg-card p-5", className)}
        >
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                <h2 id={titleId} className="text-base font-bold">
                    {title}
                </h2>
                {action}
            </div>
            {children}
        </section>
    );
}
