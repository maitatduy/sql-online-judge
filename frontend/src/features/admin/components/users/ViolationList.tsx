import type { UserViolation, ViolationSeverity } from "@/features/admin/users/types";
import { cn } from "@/lib/utils";

interface SeverityAppearance {
    label: string;
    dotClassName: string;
}

const SEVERITY_APPEARANCES: Record<ViolationSeverity, SeverityAppearance> = {
    SEVERE: { label: "Nghiêm trọng", dotClassName: "bg-violation-severe" },
    MINOR: { label: "Nhẹ", dotClassName: "bg-violation-minor" },
};

interface ViolationListProps {
    violations: UserViolation[];
}

export function ViolationList({ violations }: ViolationListProps) {
    if (violations.length === 0) {
        return <p className="text-[13px] text-muted-foreground">Chưa có vi phạm nào.</p>;
    }

    return (
        <ul className="flex flex-col gap-2 text-[13px]">
            {violations.map(({ id, severity, title, detail }) => (
                <li key={id} className="flex gap-2.5">
                    <span
                        aria-hidden="true"
                        className={cn(
                            "mt-1.5 size-2 shrink-0 rounded-full",
                            SEVERITY_APPEARANCES[severity].dotClassName,
                        )}
                    />
                    <div>
                        <span className="sr-only">{`${SEVERITY_APPEARANCES[severity].label}: `}</span>
                        <strong>{title}</strong>
                        <p className="text-xs text-muted-foreground">{detail}</p>
                    </div>
                </li>
            ))}
        </ul>
    );
}
