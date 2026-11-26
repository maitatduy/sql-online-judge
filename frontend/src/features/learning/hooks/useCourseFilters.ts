import { useSearchParams } from "next/navigation";

import { DIFFICULTIES, type Difficulty } from "@/lib/difficulty";
import { ROUTES } from "@/lib/routes";

export const COURSES_PAGE_SIZE = 6;

export interface CourseFilters {
    difficulty: Difficulty | undefined;
    page: number;
}

function difficultyOf(value: string | null): Difficulty | undefined {
    return DIFFICULTIES.find((difficulty) => difficulty === value);
}

function pageOf(value: string | null): number {
    const page = Number(value);
    return Number.isInteger(page) && page > 1 ? page : 1;
}

export function coursesHref({ difficulty, page }: CourseFilters): string {
    const params = new URLSearchParams();
    if (difficulty) {
        params.set("difficulty", difficulty);
    }
    if (page > 1) {
        params.set("page", String(page));
    }
    const query = params.toString();
    return query ? `${ROUTES.learn}?${query}` : ROUTES.learn;
}

function changeDifficulty(difficulty: Difficulty | undefined): void {
    window.history.replaceState(null, "", coursesHref({ difficulty, page: 1 }));
}

export function useCourseFilters() {
    const searchParams = useSearchParams();
    const filters: CourseFilters = {
        difficulty: difficultyOf(searchParams.get("difficulty")),
        page: pageOf(searchParams.get("page")),
    };

    return { filters, changeDifficulty };
}
