import { BookOpen, Clock, Layers } from "lucide-react";

import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

interface CourseMetaProps {
    lessonCount: number;
    estimatedMinutes: number;
    chapterCount?: number;
    className?: string;
}

export function CourseMeta({
    lessonCount,
    estimatedMinutes,
    chapterCount,
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
            className={cn(
                "flex flex-wrap items-center gap-x-4 gap-y-1 text-[13px] text-muted-foreground",
                className,
            )}
        >
            {items.map(({ icon: Icon, label }) => (
                <li key={label} className="inline-flex items-center gap-1.5">
                    <Icon aria-hidden="true" className="size-4" />
                    <span>{label}</span>
                </li>
            ))}
        </ul>
    );
}
