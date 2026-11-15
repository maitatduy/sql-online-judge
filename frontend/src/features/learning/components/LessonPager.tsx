import { ArrowLeft, ArrowRight } from "lucide-react";
import Link from "next/link";

import { ROUTES } from "@/lib/routes";

import type { LessonNeighbours } from "../lesson-navigation";

interface LessonPagerProps {
    courseSlug: string;
    neighbours: LessonNeighbours;
}

export function LessonPager({ courseSlug, neighbours: { previous, next } }: LessonPagerProps) {
    return (
        <div className="grid grid-cols-2 gap-3 border-t pt-5">
            {previous ? (
                <Link
                    href={ROUTES.lesson(courseSlug, previous.slug)}
                    className="flex flex-col gap-0.5 rounded-lg border bg-card px-4 py-3 transition-colors hover:border-brand"
                >
                    <span className="inline-flex items-center gap-1 text-[12px] text-muted-foreground">
                        <ArrowLeft aria-hidden="true" className="size-3.5" />
                        Bài trước
                    </span>
                    <span className="truncate font-semibold">{previous.title}</span>
                </Link>
            ) : (
                <span />
            )}
            {next ? (
                <Link
                    href={ROUTES.lesson(courseSlug, next.slug)}
                    className="col-start-2 flex flex-col items-end gap-0.5 rounded-lg border bg-card px-4 py-3 text-right transition-colors hover:border-brand"
                >
                    <span className="inline-flex items-center gap-1 text-[12px] text-muted-foreground">
                        Bài sau
                        <ArrowRight aria-hidden="true" className="size-3.5" />
                    </span>
                    <span className="max-w-full truncate font-semibold">{next.title}</span>
                </Link>
            ) : null}
        </div>
    );
}
