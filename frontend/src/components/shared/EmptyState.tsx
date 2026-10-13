import { cn } from "@/lib/utils";

interface EmptyStateProps {
    title: string;
    description?: string;
    icon?: React.ReactNode;
    action?: React.ReactNode;
    className?: string;
}

export function EmptyState({ title, description, icon, action, className }: EmptyStateProps) {
    return (
        <div
            className={cn(
                "rounded-lg border border-dashed bg-card px-5 py-7 text-center [&_svg:not([class*='size-'])]:size-9",
                className,
            )}
        >
            {icon ? (
                <div
                    aria-hidden="true"
                    className="mb-2.5 flex justify-center text-muted-foreground"
                >
                    {icon}
                </div>
            ) : null}
            <p className="font-bold">{title}</p>
            {description ? (
                <p className="mt-1 text-[13px] text-muted-foreground">{description}</p>
            ) : null}
            {action ? <div className="mt-3.5 flex justify-center">{action}</div> : null}
        </div>
    );
}
