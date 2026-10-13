"use client";

import { ChartPie } from "lucide-react";
import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import type { Verdict } from "@/lib/verdict";

import type { MonthlySubmissions, SubmissionTotals, VerdictShare } from "../types";
import { MonthlySubmissionChart } from "./MonthlySubmissionChart";
import { SubmissionFacts } from "./SubmissionFacts";
import { VerdictDonut } from "./VerdictDonut";
import { VerdictLegend } from "./VerdictLegend";

interface SubmissionTotalCardProps {
    totals: SubmissionTotals;
    verdictShares: VerdictShare[];
    monthly: MonthlySubmissions;
}

export function SubmissionTotalCard({ totals, verdictShares, monthly }: SubmissionTotalCardProps) {
    const [activeVerdict, setActiveVerdict] = useState<Verdict | null>(null);

    return (
        <section className="rounded-lg border bg-card px-5.5 py-5">
            <h2 className="mb-4 text-base font-bold">Tổng số bài nộp</h2>
            {totals.allTime > 0 ? (
                <>
                    <div className="flex flex-wrap items-center justify-evenly gap-x-10 gap-y-6">
                        <VerdictDonut
                            totalSubmissions={totals.allTime}
                            shares={verdictShares}
                            activeVerdict={activeVerdict}
                            onActiveVerdictChange={setActiveVerdict}
                        />
                        <div className="flex max-w-sm min-w-64 flex-1 flex-col gap-3">
                            <SubmissionFacts totals={totals} verdictShares={verdictShares} />
                            <VerdictLegend
                                shares={verdictShares}
                                activeVerdict={activeVerdict}
                                onActiveVerdictChange={setActiveVerdict}
                            />
                        </div>
                    </div>
                    <MonthlySubmissionChart monthly={monthly} />
                </>
            ) : (
                <EmptyState icon={<ChartPie />} title="Chưa có bài nộp nào" className="border-0" />
            )}
        </section>
    );
}
