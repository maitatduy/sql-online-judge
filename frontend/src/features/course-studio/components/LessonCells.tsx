"use client";

import { useState } from "react";

import { Plus } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";

import { useLessonCells } from "../hooks/useLessonCells";
import { CellCard } from "./CellCard";
import { CellForm } from "./CellForm";

interface LessonCellsProps {
    lessonId: string;
    isLocked: boolean;
}

export function LessonCells({ lessonId, isLocked }: LessonCellsProps) {
    const { data: cells, error, isPending, refetch } = useLessonCells(lessonId);
    const [isAdding, setIsAdding] = useState(false);

    return (
        <section aria-labelledby="lesson-cells-title" className="flex flex-col gap-3">
            <h2 id="lesson-cells-title" className="text-lg font-bold">
                Ô SQL chạy thử
            </h2>
            {isPending ? (
                <div role="status" aria-label="Đang tải ô SQL">
                    <Skeleton className="h-32" />
                </div>
            ) : null}
            {error && !cells ? (
                <ErrorState
                    title="Không tải được ô SQL"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {cells && cells.length === 0 ? (
                <EmptyState title="Bài giảng chưa có ô SQL nào." />
            ) : null}
            {cells?.map((cell, index) => (
                <CellCard
                    key={cell.id}
                    lessonId={lessonId}
                    cell={cell}
                    index={index + 1}
                    isLocked={isLocked}
                />
            ))}
            {isLocked || !cells ? null : isAdding ? (
                <CellForm lessonId={lessonId} onDone={() => setIsAdding(false)} />
            ) : (
                <Button
                    type="button"
                    variant="outline"
                    className="w-fit"
                    onClick={() => setIsAdding(true)}
                >
                    <Plus aria-hidden="true" />
                    Thêm ô SQL
                </Button>
            )}
        </section>
    );
}
