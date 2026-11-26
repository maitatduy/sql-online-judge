import { BookOpen, Clock, Layers } from "lucide-react";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import type { Difficulty } from "@/lib/difficulty";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

interface CourseMetaProps {
    lessonCount: number;
    estimatedMinutes: number;
    chapterCount?: number;
    difficulty?: Difficulty;
    label?: string;
    className?: string;
}

export function CourseMeta({
    lessonCount,
    estimatedMinutes,
    chapterCount,
    difficulty,
    label,
    className,
}: CourseMetaProps) {
    const items = [
        ...(chapterCount === undefined
            ? []
            : [{ icon: Layers, label: `${formatNumber(chapterCount)} chương` }]),
        { icon: BookOpen, label: `${formatNumber(lessonCount)} bài` },
        { icon: Clock, label: `${formatNumber(estimatedMinutes)} phút` },
    ];

    return (
        <ul
            aria-label={label}
            className={cn(
                "flex flex-wrap items-center gap-x-4 gap-y-1 text-[13px] text-muted-foreground",
                className,
            )}
        >
            {difficulty ? (
                <li>
                    <DifficultyLabel difficulty={difficulty} />
                </li>
            ) : null}
            {items.map(({ icon: Icon, label: itemLabel }) => (
                <li key={itemLabel} className="inline-flex items-center gap-1.5">
                    <Icon aria-hidden="true" className="size-4" />
                    <span>{itemLabel}</span>
                </li>
            ))}
        </ul>
    );
}
