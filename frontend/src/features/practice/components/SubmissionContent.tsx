import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { ROUTES } from "@/lib/routes";
import type { Verdict } from "@/lib/verdict";

import type { ProblemDetail, Submission } from "../types";
import { ActionError } from "./ActionError";
import { SubmissionVerdict } from "./SubmissionVerdict";

interface SubmissionContentProps {
    problem: ProblemDetail;
    submission: Submission | null;
    isJudging: boolean;
    error: Error | null;
    onRetry: () => void;
}

function JudgingProgress({ submission }: { submission: Submission | null }) {
    const judged = submission?.judgedTestcases ?? 0;
    const total = submission?.totalTestcases ?? 0;
    const label =
        submission?.status === "JUDGING"
            ? `Đang chấm testcase ${judged + 1}/${total}…`
            : "Đang chờ máy chấm…";

    return (
        <div className="flex flex-col gap-3">
            <p className="flex items-center gap-3 font-semibold">
                <VerdictBadge verdict="PENDING" />
                {label}
            </p>
            {total > 0 ? (
                <div
                    role="progressbar"
                    aria-label="Tiến độ chấm"
                    aria-valuemin={0}
                    aria-valuemax={total}
                    aria-valuenow={judged}
                    className="h-2 overflow-hidden rounded-full bg-surface-2"
                >
                    <span
                        className="block h-full bg-verdict-pending transition-[width]"
                        style={{ width: `${(judged / total) * 100}%` }}
                    />
                </div>
            ) : null}
        </div>
    );
}

function hasVerdict(submission: Submission): submission is Submission & { verdict: Verdict } {
    return submission.status === "COMPLETED" && submission.verdict !== null;
}

export function SubmissionContent({
    problem,
    submission,
    isJudging,
    error,
    onRetry,
}: SubmissionContentProps) {
    if (error) {
        return (
            <ActionError
                error={error}
                loginMessage="Đăng nhập để nộp bài."
                loginHref={ROUTES.loginWithNext(ROUTES.problem(problem.slug))}
                rateLimitedMessage="Bạn nộp bài quá nhanh."
                onRetry={onRetry}
            />
        );
    }
    if (isJudging) {
        return <JudgingProgress submission={submission} />;
    }
    if (submission && hasVerdict(submission)) {
        return <SubmissionVerdict problem={problem} submission={submission} />;
    }
    return (
        <p className="text-[13px] text-muted-foreground">
            Bấm Submit để nộp bài. Bài nộp được chấm trên mọi testcase, kể cả testcase ẩn.
        </p>
    );
}
