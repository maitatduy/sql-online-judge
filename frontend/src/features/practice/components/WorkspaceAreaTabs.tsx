import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { cn } from "@/lib/utils";
import type { Verdict } from "@/lib/verdict";

export type WorkspaceArea = "problem" | "code" | "result";

const AREAS: { value: WorkspaceArea; label: string }[] = [
    { value: "problem", label: "Đề" },
    { value: "code", label: "Code" },
    { value: "result", label: "Kết quả" },
];

export function workspaceAreaId(area: WorkspaceArea): string {
    return `workspace-area-${area}`;
}

interface WorkspaceAreaTabsProps {
    activeArea: WorkspaceArea;
    onAreaChange: (area: WorkspaceArea) => void;
    resultVerdict: Verdict | null;
}

export function WorkspaceAreaTabs({
    activeArea,
    onAreaChange,
    resultVerdict,
}: WorkspaceAreaTabsProps) {
    return (
        <div
            role="tablist"
            aria-label="Khu vực làm bài"
            className="flex shrink-0 border-b bg-card lg:hidden"
        >
            {AREAS.map(({ value, label }) => (
                <button
                    key={value}
                    type="button"
                    role="tab"
                    aria-selected={activeArea === value}
                    aria-controls={workspaceAreaId(value)}
                    onClick={() => onAreaChange(value)}
                    className={cn(
                        "inline-flex h-11 flex-1 items-center justify-center gap-1.5 border-b-2 text-sm font-semibold transition-colors focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none",
                        activeArea === value
                            ? "border-brand text-foreground"
                            : "border-transparent text-muted-foreground",
                    )}
                >
                    {label}
                    {value === "result" && resultVerdict ? " " : null}
                    {value === "result" && resultVerdict ? (
                        <VerdictBadge
                            verdict={resultVerdict}
                            variant="solid"
                            className="h-4.5 px-1.5 text-[10px]"
                        />
                    ) : null}
                </button>
            ))}
        </div>
    );
}
