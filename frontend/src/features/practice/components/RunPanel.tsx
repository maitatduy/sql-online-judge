import Link from "next/link";

import { ResultTable } from "@/components/shared/ResultTable";
import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { Button } from "@/components/ui/button";
import { ApiRequestError, isUnauthenticatedError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { VERDICT_LABELS } from "@/lib/verdict";

import type { CodeRun, ProblemDetail } from "../types";

const MS_PER_SECOND = 1000;
const RATE_LIMITED_STATUS = 429;

interface RunPanelProps {
    problem: ProblemDetail;
    run: CodeRun | null;
    isRunning: boolean;
    error: Error | null;
    onRetry: () => void;
}

function RunError({
    error,
    problem,
    onRetry,
}: Pick<RunPanelProps, "problem" | "onRetry"> & { error: Error }) {
    if (isUnauthenticatedError(error)) {
        return (
            <p className="text-[13px]">
                Đăng nhập để chạy thử.{" "}
                <Link
                    href={ROUTES.loginWithNext(ROUTES.problem(problem.slug))}
                    className="font-semibold text-brand"
                >
                    Đăng nhập
                </Link>
            </p>
        );
    }
    if (error instanceof ApiRequestError && error.status === RATE_LIMITED_STATUS) {
        return (
            <p role="alert" className="text-[13px]">
                {error.retryAfterSeconds
                    ? `Bạn chạy thử quá nhanh. Thử lại sau ${error.retryAfterSeconds} giây.`
                    : "Bạn chạy thử quá nhanh. Thử lại sau ít phút."}
            </p>
        );
    }
    return (
        <div role="alert" className="flex items-center gap-3 text-[13px]">
            <span>{error.message}</span>
            <Button type="button" size="sm" variant="outline" onClick={onRetry}>
                Thử lại
            </Button>
        </div>
    );
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

function RunContent({ problem, run, isRunning, error, onRetry }: RunPanelProps) {
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
        return <RunError error={error} problem={problem} onRetry={onRetry} />;
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

export function RunPanel(props: RunPanelProps) {
    return (
        <section
            aria-labelledby="run-panel-title"
            className="flex h-2/5 min-h-40 shrink-0 flex-col border-t bg-card"
        >
            <h2 id="run-panel-title" className="border-b px-4 py-2.5 text-[13px] font-semibold">
                Kết quả chạy thử
            </h2>
            <div className="min-h-0 flex-1 overflow-auto px-4 py-3">
                <RunContent {...props} />
            </div>
        </section>
    );
}
