import { ResultTable } from "@/components/shared/ResultTable";
import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { VERDICT_LABELS } from "@/lib/verdict";

import type { CodeRun, ProblemDetail } from "../types";
import { ActionError } from "./ActionError";

const MS_PER_SECOND = 1000;

interface RunContentProps {
    problem: ProblemDetail;
    run: CodeRun | null;
    isRunning: boolean;
    error: Error | null;
    onRetry: () => void;
}

function RunOutcome({ run, problem }: { run: CodeRun; problem: ProblemDetail }) {
    if (run.outcome === "OK" && run.result) {
        return (
            <div className="flex flex-col gap-2">
                <p className="text-xs text-muted-foreground">
                    {`${formatNumber(run.result.rows.length)} dòng · ${formatNumber(run.runtimeMs ?? 0)} ms`}
                </p>
                <ResultTable label="Kết quả chạy thử" result={run.result} />
            </div>
        );
    }
    if (run.outcome === "OK" || run.outcome === null) {
        return null;
    }
    const message =
        run.outcome === "TLE"
            ? `Quá giới hạn ${formatNumber(problem.timeLimitMs / MS_PER_SECOND)} giây.`
            : run.errorMessage;
    return (
        <div className="flex flex-col gap-2">
            <p className="flex items-center gap-2 font-semibold">
                <VerdictBadge verdict={run.outcome} variant="solid" />
                {VERDICT_LABELS[run.outcome]}
            </p>
            {message ? (
                <pre className="rounded-lg border bg-background px-4 py-3 font-mono text-[13px] whitespace-pre-wrap">
                    {message}
                </pre>
            ) : null}
        </div>
    );
}

export function RunContent({ problem, run, isRunning, error, onRetry }: RunContentProps) {
    if (isRunning) {
        return (
            <p
                role="status"
                aria-label="Đang chạy thử"
                className="text-[13px] text-muted-foreground"
            >
                Đang chạy thử trên dữ liệu mẫu…
            </p>
        );
    }
    if (error) {
        return (
            <ActionError
                error={error}
                loginMessage="Đăng nhập để chạy thử."
                loginHref={ROUTES.loginWithNext(ROUTES.problem(problem.slug))}
                rateLimitedMessage="Bạn chạy thử quá nhanh."
                onRetry={onRetry}
            />
        );
    }
    if (run) {
        return <RunOutcome run={run} problem={problem} />;
    }
    return (
        <p className="text-[13px] text-muted-foreground">
            Bấm Run hoặc Ctrl+Enter để chạy thử trên dữ liệu mẫu. Chạy thử không tính là lần nộp.
        </p>
    );
}
