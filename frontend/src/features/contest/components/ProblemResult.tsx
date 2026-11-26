import { CircleCheck, CircleX, LoaderCircle } from "lucide-react";

import { contestProblemStatusOf, type ContestProblemAttempts } from "@/lib/contest";
import { formatNumber } from "@/lib/format";

const ROW_CLASSES = {
    ACCEPTED: "bg-verdict-ac/12",
    WRONG: "bg-verdict-wa/12",
    PENDING: "",
} as const;

export function resultRowClassOf(cell: ContestProblemAttempts | undefined): string | undefined {
    const status = contestProblemStatusOf(cell);
    return status ? ROW_CLASSES[status] : undefined;
}

interface ProblemResultProps {
    cell: ContestProblemAttempts | undefined;
}

export function ProblemResult({ cell }: ProblemResultProps) {
    const status = contestProblemStatusOf(cell);
    if (status === "ACCEPTED") {
        return (
            <span className="inline-flex items-center gap-1.5 font-semibold text-verdict-ac">
                <CircleCheck aria-hidden="true" className="size-4" />
                Đã AC
            </span>
        );
    }
    if (status === "WRONG" && cell) {
        return (
            <span className="inline-flex items-center gap-1.5 font-semibold text-verdict-wa">
                <CircleX aria-hidden="true" className="size-4" />
                {`Sai ${formatNumber(cell.wrongAttempts)} lần`}
            </span>
        );
    }
    if (status === "PENDING") {
        return (
            <span className="inline-flex items-center gap-1.5 font-semibold text-verdict-pending">
                <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
                Đang chấm
            </span>
        );
    }
    return <span className="text-muted-foreground">Chưa nộp</span>;
}
