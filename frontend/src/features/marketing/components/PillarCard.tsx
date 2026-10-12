import type { LucideIcon } from "lucide-react";

import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface PillarCardProps {
    icon: LucideIcon;
    iconClassName: string;
    title: string;
    description: string;
    children: React.ReactNode;
}

export function PillarCard({
    icon: Icon,
    iconClassName,
    title,
    description,
    children,
}: PillarCardProps) {
    return (
        <Card className="gap-3.5 p-6">
            <span
                aria-hidden="true"
                className={cn(
                    "inline-flex size-10 items-center justify-center rounded-lg",
                    iconClassName,
                )}
            >
                <Icon className="size-5" />
            </span>
            <h3 className="text-xl font-bold">{title}</h3>
            <p className="text-muted-foreground">{description}</p>
            <div className="mt-auto rounded-lg border bg-surface p-3 text-xs">{children}</div>
        </Card>
    );
}
