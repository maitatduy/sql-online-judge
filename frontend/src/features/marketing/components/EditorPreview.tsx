import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { cn } from "@/lib/utils";
import type { Verdict } from "@/lib/verdict";

type EditorPreviewSize = "md" | "lg";

interface EditorPreviewProps {
    label: string;
    fileName: string;
    verdict?: Verdict;
    verdictDetail: string;
    lineCount: number;
    columns: string[];
    rows: string[][];
    size?: EditorPreviewSize;
    className?: string;
    children: React.ReactNode;
}

const SIZE_CLASSES: Record<
    EditorPreviewSize,
    { bar: string; code: string; table: string; cell: string }
> = {
    md: { bar: "h-10", code: "text-[13.5px] leading-[22px]", table: "text-xs", cell: "px-3 py-2" },
    lg: { bar: "h-12", code: "text-base leading-7", table: "text-sm", cell: "px-4 py-3" },
};

export function EditorPreview({
    label,
    fileName,
    verdict = "AC",
    verdictDetail,
    lineCount,
    columns,
    rows,
    size = "md",
    className,
    children,
}: EditorPreviewProps) {
    const sizeClasses = SIZE_CLASSES[size];
    const lineNumbers = Array.from({ length: lineCount }, (_, index) => index + 1);

    return (
        <figure
            aria-label={label}
            className={cn(
                "dark min-w-0 overflow-hidden rounded-xl border bg-background font-mono text-foreground shadow-2xl",
                className,
            )}
        >
            <div
                className={cn(
                    "flex items-center gap-2 border-b bg-surface px-3.5",
                    sizeClasses.bar,
                )}
            >
                <span aria-hidden="true" className="flex gap-2">
                    <span className="size-2 rounded-full bg-muted-foreground/40" />
                    <span className="size-2 rounded-full bg-muted-foreground/40" />
                    <span className="size-2 rounded-full bg-muted-foreground/40" />
                </span>
                <span className="ml-2.5 min-w-0 truncate text-xs text-muted-foreground">
                    {fileName}
                </span>
                <VerdictBadge
                    verdict={verdict}
                    detail={verdictDetail}
                    variant="solid"
                    className="ml-auto shrink-0"
                />
            </div>
            <div className={cn("flex gap-3.5 overflow-x-auto py-4", sizeClasses.code)}>
                <div
                    aria-hidden="true"
                    className="w-9 shrink-0 text-right text-muted-foreground/60 select-none"
                >
                    {lineNumbers.map((lineNumber) => (
                        <div key={lineNumber}>{lineNumber}</div>
                    ))}
                </div>
                <pre className="pr-4">
                    <code>{children}</code>
                </pre>
            </div>
            <div className="overflow-x-auto border-t bg-surface">
                <table className={cn("w-full border-collapse", sizeClasses.table)}>
                    <caption className="sr-only">Kết quả truy vấn</caption>
                    <thead>
                        <tr>
                            {columns.map((column) => (
                                <th
                                    key={column}
                                    scope="col"
                                    className={cn(
                                        "border-b text-left font-semibold text-muted-foreground",
                                        sizeClasses.cell,
                                    )}
                                >
                                    {column}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {rows.length === 0 && (
                            <tr>
                                <td
                                    colSpan={columns.length}
                                    className={cn(
                                        "border-b text-muted-foreground",
                                        sizeClasses.cell,
                                    )}
                                >
                                    0 dòng
                                </td>
                            </tr>
                        )}
                        {rows.map((row) => (
                            <tr key={row.join("|")}>
                                {row.map((cell, cellIndex) => (
                                    <td
                                        key={columns[cellIndex]}
                                        className={cn("border-b", sizeClasses.cell)}
                                    >
                                        {cell}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </figure>
    );
}
