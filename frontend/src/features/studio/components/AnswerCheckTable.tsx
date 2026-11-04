import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { DIALECT_LABELS, type Dialect } from "@/lib/dialect";
import { cn } from "@/lib/utils";

import type { AnswerCheckTestcase, DialectCheckResult, MismatchReason } from "../types";

const DIALECT_ORDER: readonly Dialect[] = ["POSTGRESQL", "MYSQL", "SQLITE"];
const SYSTEM_ERROR_MESSAGE = "Máy chấm gặp lỗi khi chạy testcase này. Thử lại sau.";
const NOT_COMPARED_MESSAGE = "Không so được vì đáp án PostgreSQL bị lỗi";
const MISMATCH_LABELS: Record<MismatchReason, string | undefined> = {
    NONE: undefined,
    COLUMN_COUNT: "Sai số cột",
    COLUMN_NAMES: "Sai tên cột",
    ROWS: "Sai dữ liệu",
    ORDER: "Sai thứ tự dòng",
};

interface AnswerCheckTableProps {
    testcases: AnswerCheckTestcase[];
}

function dialectsOf(testcases: AnswerCheckTestcase[]): Dialect[] {
    return DIALECT_ORDER.filter((dialect) =>
        testcases.some((testcase) =>
            testcase.dialects.some((result) => result.dialect === dialect),
        ),
    );
}

function DialectCell({ result }: { result: DialectCheckResult | undefined }) {
    if (!result) {
        return <span className="text-muted-foreground">-</span>;
    }
    if (result.verdict === null) {
        return <span className="text-muted-foreground">{NOT_COMPARED_MESSAGE}</span>;
    }
    return (
        <div className="flex flex-col items-start gap-1">
            <VerdictBadge verdict={result.verdict} detail={MISMATCH_LABELS[result.mismatch]} />
            {result.message ? (
                <span className="font-mono text-xs break-all text-destructive">
                    {result.message}
                </span>
            ) : null}
        </div>
    );
}

export function AnswerCheckTable({ testcases }: AnswerCheckTableProps) {
    const dialects = dialectsOf(testcases);

    return (
        <div className="overflow-x-auto rounded-md border">
            <table
                aria-label="Kết quả kiểm tra đáp án"
                className="w-full border-collapse text-[13px]"
            >
                <thead className="bg-surface-2">
                    <tr>
                        <th scope="col" className="px-3 py-1.5 text-left font-semibold">
                            Testcase
                        </th>
                        {dialects.map((dialect) => (
                            <th
                                key={dialect}
                                scope="col"
                                className="px-3 py-1.5 text-left font-semibold whitespace-nowrap"
                            >
                                {DIALECT_LABELS[dialect]}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {testcases.map((testcase) => (
                        <tr
                            key={testcase.testcaseId}
                            className={cn("border-t", !testcase.isPassed && "bg-destructive/5")}
                        >
                            <td className="px-3 py-2 align-top whitespace-nowrap">
                                <span className="font-semibold">Testcase {testcase.position}</span>
                                <span className="ml-2 rounded-full bg-surface-2 px-2 py-0.5 text-xs text-muted-foreground">
                                    {testcase.isPublic ? "Công khai" : "Ẩn"}
                                </span>
                            </td>
                            {testcase.systemError ? (
                                <td
                                    colSpan={Math.max(dialects.length, 1)}
                                    className="px-3 py-2 align-top text-destructive"
                                >
                                    {SYSTEM_ERROR_MESSAGE}
                                </td>
                            ) : (
                                dialects.map((dialect) => (
                                    <td key={dialect} className="px-3 py-2 align-top">
                                        <DialectCell
                                            result={testcase.dialects.find(
                                                (result) => result.dialect === dialect,
                                            )}
                                        />
                                    </td>
                                ))
                            )}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
