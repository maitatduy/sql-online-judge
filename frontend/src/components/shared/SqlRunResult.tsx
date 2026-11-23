import { ResultTable } from "@/components/shared/ResultTable";
import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import type { ResultSet } from "@/lib/result-set";
import { VERDICT_LABELS } from "@/lib/verdict";

export interface SqlRun {
    outcome: "OK" | "RE" | "TLE" | "FS" | "SE" | null;
    result: ResultSet | null;
    errorMessage: string | null;
    runtimeMs: number | null;
}

const SYSTEM_ERROR_MESSAGE = "Máy chấm đang bận, thử lại sau ít phút.";
const TIMED_OUT_MESSAGE = "Câu lệnh chạy quá thời gian cho phép.";
const DEFAULT_RETRY_SECONDS = 60;

interface SqlRunResultProps {
    run: SqlRun | null;
    isRunning: boolean;
    error: Error | null;
}

function errorMessageOf(error: Error): string {
    if (!(error instanceof ApiRequestError)) {
        return error.message;
    }
    if (error.status === 401) {
        return "Đăng nhập để chạy SQL.";
    }
    if (error.status === 429) {
        const seconds = error.retryAfterSeconds ?? DEFAULT_RETRY_SECONDS;
        return `Chạy quá nhanh, thử lại sau ${formatNumber(seconds)} giây.`;
    }
    return error.message;
}

export function SqlRunResult({ run, isRunning, error }: SqlRunResultProps) {
    if (isRunning) {
        return (
            <p role="status" className="text-[13px] text-muted-foreground">
                Đang chạy…
            </p>
        );
    }
    if (error) {
        return (
            <p role="alert" className="text-[13px] text-destructive">
                {errorMessageOf(error)}
            </p>
        );
    }
    if (!run?.outcome) {
        return null;
    }
    if (run.outcome === "OK") {
        return run.result ? (
            <div className="flex flex-col gap-2">
                <p className="text-[12px] text-muted-foreground">
                    {`${formatNumber(run.result.rows.length)} dòng, ${formatNumber(run.runtimeMs ?? 0)} ms`}
                </p>
                <ResultTable label="Kết quả" result={run.result} />
            </div>
        ) : null;
    }
    const message =
        run.outcome === "SE"
            ? SYSTEM_ERROR_MESSAGE
            : run.outcome === "TLE"
              ? TIMED_OUT_MESSAGE
              : run.errorMessage;
    return (
        <div className="flex flex-col gap-2">
            <p className="flex items-center gap-2 text-[13px] font-semibold">
                <VerdictBadge verdict={run.outcome} variant="solid" />
                {VERDICT_LABELS[run.outcome]}
            </p>
            {message ? (
                <pre className="rounded-md border bg-surface-2 px-3 py-2 font-mono text-[13px] whitespace-pre-wrap">
                    {message}
                </pre>
            ) : null}
        </div>
    );
}
