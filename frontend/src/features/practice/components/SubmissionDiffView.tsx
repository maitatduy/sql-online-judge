import { ResultTable, type RowMark } from "@/components/shared/ResultTable";
import { formatNumber } from "@/lib/format";
import type { ResultSet } from "@/lib/result-set";

import type { DiffCell, OrderMismatch, SubmissionDiff } from "../types";

export const DIFF_ROW_LIMIT = 20;

function resultOf(columns: string[], rows: DiffCell[][]): ResultSet {
    return {
        columns,
        rows: rows.map((row) => row.map((cell) => (cell.kind === "NULL" ? null : cell.value))),
        isTruncated: false,
    };
}

function marked(rows: DiffCell[][], mark: RowMark): Partial<Record<number, RowMark>> {
    return Object.fromEntries(rows.map((_, rowIndex) => [rowIndex, mark]));
}

function ColumnsDiff({ diff }: { diff: SubmissionDiff }) {
    return (
        <dl className="grid gap-1 text-[13px]">
            <div>
                <dt className="inline text-muted-foreground">
                    Mong đợi {formatNumber(diff.expectedColumns.length)} cột:{" "}
                </dt>
                <dd className="inline font-mono">{diff.expectedColumns.join(", ")}</dd>
            </div>
            <div>
                <dt className="inline text-muted-foreground">
                    Kết quả của bạn có {formatNumber(diff.actualColumns.length)} cột:{" "}
                </dt>
                <dd className="inline font-mono">{diff.actualColumns.join(", ")}</dd>
            </div>
        </dl>
    );
}

function OrderDiff({ columns, mismatch }: { columns: string[]; mismatch: OrderMismatch }) {
    const position = formatNumber(mismatch.rowNumber);
    return (
        <div className="grid gap-3 xl:grid-cols-2">
            <ResultTable
                label={`Dòng ${position} của bạn`}
                result={resultOf(columns, [mismatch.actual])}
            />
            <ResultTable
                label={`Dòng ${position} mong đợi`}
                result={resultOf(columns, [mismatch.expected])}
            />
        </div>
    );
}

function RowsDiff({ diff }: { diff: SubmissionDiff }) {
    const isLimited =
        diff.missingRows.length >= DIFF_ROW_LIMIT || diff.extraRows.length >= DIFF_ROW_LIMIT;
    return (
        <div className="flex flex-col gap-3">
            <div className="grid gap-3 xl:grid-cols-2">
                {diff.extraRows.length > 0 ? (
                    <ResultTable
                        label="Dòng thừa trong kết quả của bạn"
                        result={resultOf(diff.actualColumns, diff.extraRows)}
                        rowMarks={marked(diff.extraRows, "extra")}
                    />
                ) : null}
                {diff.missingRows.length > 0 ? (
                    <ResultTable
                        label="Dòng thiếu so với kết quả mong đợi"
                        result={resultOf(diff.expectedColumns, diff.missingRows)}
                        rowMarks={marked(diff.missingRows, "missing")}
                    />
                ) : null}
            </div>
            {isLimited ? (
                <p className="text-[13px] text-muted-foreground">
                    Chỉ hiện tối đa {DIFF_ROW_LIMIT} dòng thiếu và {DIFF_ROW_LIMIT} dòng thừa.
                </p>
            ) : null}
        </div>
    );
}

export function SubmissionDiffView({ diff }: { diff: SubmissionDiff }) {
    return (
        <div className="flex flex-col gap-3">
            <p className="text-[13px] text-muted-foreground">
                Mong đợi {formatNumber(diff.expectedRowCount)} dòng, kết quả của bạn có{" "}
                {formatNumber(diff.actualRowCount)} dòng.
            </p>
            {diff.reason === "COLUMN_COUNT" || diff.reason === "COLUMN_NAMES" ? (
                <ColumnsDiff diff={diff} />
            ) : null}
            {diff.reason === "ROWS" ? <RowsDiff diff={diff} /> : null}
            {diff.orderMismatch ? (
                <OrderDiff columns={diff.expectedColumns} mismatch={diff.orderMismatch} />
            ) : null}
        </div>
    );
}
