import type { UserViolation, ViolationSeverity } from "@/features/admin/users/types";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";

import { CaseStatusPill } from "./CaseStatusPill";

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

function handledLabel({ handledBy, handledAt }: UserViolation): string | null {
    if (!handledBy || !handledAt) {
        return null;
    }
    return `Xử lý bởi ${handledBy} · ${formatDate(handledAt)}`;
}

export function ViolationList({ violations }: ViolationListProps) {
    if (violations.length === 0) {
        return <p className="text-[13px] text-muted-foreground">Chưa có vi phạm nào.</p>;
    }

    return (
        <ul className="flex flex-col gap-3 text-[13px]">
            {violations.map((violation) => (
                <li key={violation.id} className="flex gap-2.5">
                    <span
                        aria-hidden="true"
                        className={cn(
                            "mt-1.5 size-2 shrink-0 rounded-full",
                            SEVERITY_APPEARANCES[violation.severity].dotClassName,
                        )}
                    />
                    <div className="min-w-0 flex-1">
                        <span className="sr-only">
                            {`${SEVERITY_APPEARANCES[violation.severity].label}: `}
                        </span>
                        <strong>{violation.title}</strong>
                        <p className="text-xs text-muted-foreground">{violation.detail}</p>
                        {handledLabel(violation) ? (
                            <p className="text-xs text-muted-foreground">
                                {handledLabel(violation)}
                            </p>
                        ) : null}
                    </div>
                    <CaseStatusPill status={violation.status} />
                </li>
            ))}
        </ul>
    );
}
