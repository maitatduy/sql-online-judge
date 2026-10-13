"use client";

import { LineChart } from "lucide-react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useIsProfileOwner } from "../hooks/useIsProfileOwner";
import { useRatingHistory } from "../hooks/useRatingHistory";

import type { RatingPoint } from "../types";

import { RatingSummary } from "./RatingSummary";

const CHART_PLACEHOLDER_CLASS_NAME = "mt-3 h-[190px]";

const RatingChart = dynamic(
    () => import("./RatingChart").then((chartModule) => chartModule.RatingChart),
    { ssr: false, loading: () => <Skeleton className={CHART_PLACEHOLDER_CLASS_NAME} /> },
);

interface RatingHistoryCardProps {
    username: string;
}

function RatingHistory({ points }: { points: RatingPoint[] }) {
    const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

    return (
        <>
            <RatingSummary points={points} activeIndex={hoveredIndex ?? points.length - 1} />
            <div aria-hidden="true" className="mt-3">
                <RatingChart points={points} onActivePointChange={setHoveredIndex} />
            </div>
            <table className="sr-only">
                <caption>Rating sau từng contest</caption>
                <thead>
                    <tr>
                        <th scope="col">Contest</th>
                        <th scope="col">Rating</th>
                        <th scope="col">Hạng</th>
                    </tr>
                </thead>
                <tbody>
                    {points.map((point) => (
                        <tr key={point.contestSlug}>
                            <th scope="row">{point.contestTitle}</th>
                            <td>{formatNumber(point.rating)}</td>
                            <td>#{formatNumber(point.rank)}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </>
    );
}

export function RatingHistoryCard({ username }: RatingHistoryCardProps) {
    const { data: points, error, isPending, refetch } = useRatingHistory(username);
    const isOwner = useIsProfileOwner(username);

    return (
        <section className="min-w-0 flex-[2_1_380px] rounded-lg border bg-card p-5">
            {isPending ? (
                <div role="status" aria-busy="true" aria-label="Đang tải lịch sử rating">
                    <Skeleton className="h-14 w-2/5" />
                    <Skeleton className={CHART_PLACEHOLDER_CLASS_NAME} />
                </div>
            ) : null}
            {error && !points ? (
                <ErrorState
                    title="Không tải được lịch sử rating"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="border-0 p-0"
                />
            ) : null}
            {points && points.length > 0 ? <RatingHistory points={points} /> : null}
            {points && points.length === 0 ? (
                <>
                    <h2 className="mb-3 text-base font-bold">Rating contest</h2>
                    <EmptyState
                        icon={<LineChart />}
                        title="Chưa thi contest nào"
                        description="Rating sẽ xuất hiện sau contest đầu tiên."
                        action={
                            isOwner ? (
                                <Button asChild size="sm">
                                    <Link href={ROUTES.contests}>Xem contest</Link>
                                </Button>
                            ) : undefined
                        }
                    />
                </>
            ) : null}
        </section>
    );
}
