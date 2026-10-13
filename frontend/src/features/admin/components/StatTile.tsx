import { useId } from "react";

interface StatTileProps {
    label: string;
    value: React.ReactNode;
    children: React.ReactNode;
}

export function StatTile({ label, value, children }: StatTileProps) {
    const labelId = useId();

    return (
        <div role="group" aria-labelledby={labelId} className="rounded-lg border bg-card p-4.5">
            <p id={labelId} className="text-[13px] text-muted-foreground">
                {label}
            </p>
            <div className="mt-1.5 font-mono text-[28px] leading-tight font-extrabold">{value}</div>
            {children}
        </div>
    );
}
