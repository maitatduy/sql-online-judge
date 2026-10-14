import { cn } from "@/lib/utils";

interface SummaryTileProps {
    label: string;
    value: React.ReactNode;
    isSelected: boolean;
    onSelect: () => void;
    valueClassName?: string;
}

export function SummaryTile({
    label,
    value,
    isSelected,
    onSelect,
    valueClassName,
}: SummaryTileProps) {
    return (
        <button
            type="button"
            aria-pressed={isSelected}
            onClick={onSelect}
            className={cn(
                "rounded-lg border bg-card px-4 py-3.5 text-left transition-colors hover:border-brand/40",
                isSelected ? "border-brand ring-[3px] ring-brand/20 hover:border-brand" : null,
            )}
        >
            <span className="block text-xs text-muted-foreground">{label}</span>
            <span
                className={cn("mt-0.5 block font-mono text-[22px] font-extrabold", valueClassName)}
            >
                {value}
            </span>
        </button>
    );
}
