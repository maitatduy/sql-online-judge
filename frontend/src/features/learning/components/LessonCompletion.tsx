import { Check, CircleCheck } from "lucide-react";

import { Button } from "@/components/ui/button";

import { useCompleteLesson } from "../hooks/useCompleteLesson";

interface LessonCompletionProps {
    courseSlug: string;
    lessonSlug: string;
    isCompleted: boolean;
}

export function LessonCompletion({ courseSlug, lessonSlug, isCompleted }: LessonCompletionProps) {
    const completeLesson = useCompleteLesson(courseSlug);

    if (isCompleted) {
        return (
            <p className="inline-flex items-center gap-1.5 self-start rounded-full bg-brand-soft px-3 py-1.5 text-[13px] font-semibold text-brand">
                <CircleCheck aria-hidden="true" className="size-4" />
                Đã hoàn thành
            </p>
        );
    }

    return (
        <div className="flex flex-wrap items-center gap-3">
            <Button
                type="button"
                disabled={completeLesson.isPending}
                onClick={() => completeLesson.mutate(lessonSlug)}
            >
                <Check aria-hidden="true" />
                Hoàn thành bài học
            </Button>
            {completeLesson.isError ? (
                <p role="alert" className="text-[13px] text-destructive">
                    Chưa lưu được, thử lại
                </p>
            ) : null}
        </div>
    );
}
