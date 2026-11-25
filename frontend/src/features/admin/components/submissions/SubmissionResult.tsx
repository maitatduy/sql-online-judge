import { VerdictBadge } from "@/components/shared/VerdictBadge";

import type { AdminSubmission } from "../../submissions/types";

interface SubmissionResultProps {
    submission: Pick<AdminSubmission, "status" | "verdict">;
}

export function SubmissionResult({ submission }: SubmissionResultProps) {
    return submission.status === "COMPLETED" && submission.verdict ? (
        <VerdictBadge verdict={submission.verdict} />
    ) : (
        <VerdictBadge verdict="PENDING" />
    );
}
