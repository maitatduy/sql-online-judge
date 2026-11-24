"use client";

import { useSearchParams } from "next/navigation";

import { EmptyState } from "@/components/shared/EmptyState";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { cn } from "@/lib/utils";

import { REPORT_STATUS_LABELS, REPORT_STATUSES } from "../../reports/labels";
import {
    readReportSelection,
    type ReportSelection,
    reportsHref,
} from "../../reports/search-params";
import { ReportCaseDetailView } from "./ReportCaseDetailView";
import { ReportCaseList } from "./ReportCaseList";

function select(selection: ReportSelection): void {
    window.history.replaceState(null, "", reportsHref(selection));
}

export function AdminReports() {
    const selection = readReportSelection(useSearchParams());

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <h1 className="text-[24px] font-extrabold tracking-[-0.02em]">Báo cáo vi phạm</h1>
            <div
                role="group"
                aria-label="Lọc theo trạng thái"
                className="flex w-fit flex-wrap gap-1 rounded-lg border bg-card p-1"
            >
                {REPORT_STATUSES.map((status) => (
                    <button
                        key={status}
                        type="button"
                        aria-pressed={status === selection.status}
                        onClick={() => select({ status, selected: null })}
                        className={cn(
                            "min-h-9 rounded-md px-3 text-sm font-semibold",
                            status === selection.status
                                ? "bg-brand-soft text-brand"
                                : "text-muted-foreground hover:bg-surface-2",
                        )}
                    >
                        {REPORT_STATUS_LABELS[status]}
                    </button>
                ))}
            </div>
            <div className="grid items-start gap-4 lg:grid-cols-[380px_minmax(0,1fr)]">
                <ReportCaseList
                    status={selection.status}
                    selected={selection.selected}
                    onSelect={(selected) => select({ ...selection, selected })}
                />
                {selection.selected ? (
                    <ReportCaseDetailView
                        key={`${selection.selected.targetType}-${selection.selected.targetId}`}
                        caseKey={selection.selected}
                        isOpen={selection.status === "OPEN"}
                        onResolved={() => select({ ...selection, selected: null })}
                    />
                ) : (
                    <section aria-label="Chi tiết báo cáo" className="rounded-lg border bg-card">
                        <EmptyState title="Chọn một báo cáo để xem." className="border-0" />
                    </section>
                )}
            </div>
        </main>
    );
}
