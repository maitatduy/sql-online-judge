import { formatNumber } from "@/lib/format";

interface CourseProgressBarProps {
    completedCount: number;
    lessonCount: number;
}

const FULL_PERCENT = 100;

export function CourseProgressBar({ completedCount, lessonCount }: CourseProgressBarProps) {
    const percent =
        lessonCount === 0 ? 0 : Math.round((completedCount / lessonCount) * FULL_PERCENT);

    return (
        <div className="flex items-center gap-3">
            <div
                role="progressbar"
                aria-label="Tiến độ khoá học"
                aria-valuemin={0}
                aria-valuemax={FULL_PERCENT}
                aria-valuenow={percent}
                className="h-2 flex-1 overflow-hidden rounded-full bg-surface-2"
            >
                <div className="h-full rounded-full bg-brand" style={{ width: `${percent}%` }} />
            </div>
            <span className="text-[13px] font-semibold text-muted-foreground">
                {formatNumber(completedCount)}/{formatNumber(lessonCount)} bài
            </span>
        </div>
    );
}
