import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { type MonthlyBar, monthlyBars } from "../monthly-submissions";
import type { MonthlySubmissions } from "../types";

interface MonthlySubmissionChartProps {
    monthly: MonthlySubmissions;
}

interface MonthColumnProps {
    bar: MonthlyBar;
    year: number;
}

function MonthColumn({ bar, year }: MonthColumnProps) {
    const count = bar.count === null ? null : formatNumber(bar.count);

    return (
        <li
            title={count === null ? undefined : `Tháng ${bar.month} năm ${year}: ${count} bài nộp`}
            className="group flex min-w-0 flex-1 flex-col items-center"
        >
            <span className="sr-only">
                Tháng {bar.month}: {count === null ? "Chưa có số liệu" : `${count} bài nộp`}
            </span>
            <div aria-hidden="true" className="w-full flex-1 border-b px-1 pt-5 sm:px-2">
                <div className="relative flex h-full items-end justify-center">
                    {count === null ? null : (
                        <>
                            <span
                                className={cn(
                                    "absolute mb-1 hidden font-mono text-[11px] whitespace-nowrap text-muted-foreground md:block",
                                    bar.isPeak && "font-bold text-foreground",
                                )}
                                style={{ bottom: `${bar.heightPercent}%` }}
                            >
                                {count}
                            </span>
                            <span
                                data-testid="month-column"
                                className={cn(
                                    "block min-h-0.75 w-full max-w-12 rounded-t-[3px] bg-chart-1 transition-opacity group-hover:opacity-100 motion-reduce:transition-none",
                                    !bar.isPeak && "opacity-40",
                                )}
                                style={{ height: `${bar.heightPercent}%` }}
                            />
                        </>
                    )}
                </div>
            </div>
            <span aria-hidden="true" className="mt-1.5 font-mono text-[11px] text-muted-foreground">
                T{bar.month}
            </span>
        </li>
    );
}

export function MonthlySubmissionChart({ monthly }: MonthlySubmissionChartProps) {
    return (
        <div className="mt-5 border-t pt-4">
            <h3 className="text-[13px] font-semibold text-muted-foreground">
                Bài nộp theo tháng năm {monthly.year}
            </h3>
            <ul aria-label={`Số bài nộp theo tháng năm ${monthly.year}`} className="mt-2 flex h-52">
                {monthlyBars(monthly.months).map((bar) => (
                    <MonthColumn key={bar.month} bar={bar} year={monthly.year} />
                ))}
            </ul>
        </div>
    );
}
