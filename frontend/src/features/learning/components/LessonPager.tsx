import { ArrowLeft, ArrowRight } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import type { LessonNeighbours } from "../lesson-navigation";

interface LessonPagerProps {
    courseSlug: string;
    neighbours: LessonNeighbours;
}

export function LessonPager({ courseSlug, neighbours: { previous, next } }: LessonPagerProps) {
    return (
        <nav aria-label="Chuyển bài" className="flex flex-wrap items-center gap-2">
            {previous ? (
                <Button asChild variant="outline">
                    <Link href={ROUTES.lesson(courseSlug, previous.slug)} title={previous.title}>
                        <ArrowLeft aria-hidden="true" />
                        Bài trước
                    </Link>
                </Button>
            ) : null}
            {next ? (
                <Button asChild>
                    <Link href={ROUTES.lesson(courseSlug, next.slug)} title={next.title}>
                        Bài sau
                        <ArrowRight aria-hidden="true" />
                    </Link>
                </Button>
            ) : null}
        </nav>
    );
}
