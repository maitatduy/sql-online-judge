import { formatNumber } from "@/lib/format";

import type { VariantRunResult } from "../types";
import { RunResultTable } from "./RunResultTable";

const SANDBOX_FAILED_MESSAGE = "Máy chấm gặp lỗi khi chạy bộ dữ liệu này. Thử lại sau.";

interface VariantRunCardProps {
    result: VariantRunResult;
}

function rowSummaryOf(result: VariantRunResult): string {
    if (result.truncated) {
        return `Hiển thị ${formatNumber(result.rows.length)} trong ${formatNumber(result.rowCount)} dòng`;
    }
    return `${formatNumber(result.rowCount)} dòng`;
}

export function VariantRunCard({ result }: VariantRunCardProps) {
    return (
        <section
            aria-label={`Bộ dữ liệu ${result.variantName}`}
            className="flex flex-col gap-3 rounded-md border p-4"
        >
            <div className="flex flex-wrap items-center gap-2">
                <p className="text-sm font-semibold">{result.variantName}</p>
                <span className="rounded-full bg-surface-2 px-2 py-0.5 text-xs text-muted-foreground">
                    {result.isPublic ? "Công khai" : "Ẩn"}
                </span>
                <span className="ml-auto text-xs text-muted-foreground">
                    {formatNumber(result.runtimeMs)} ms
                </span>
            </div>
            {result.status === "SUCCEEDED" ? (
                <>
                    <RunResultTable
                        label={`Kết quả bộ ${result.variantName}`}
                        columns={result.columns}
                        rows={result.rows}
                    />
                    <p className="text-[13px] text-muted-foreground">{rowSummaryOf(result)}</p>
                </>
            ) : (
                <p className="text-[13px] text-destructive">
                    {result.status === "SANDBOX_FAILED" ? SANDBOX_FAILED_MESSAGE : result.message}
                </p>
            )}
        </section>
    );
}
