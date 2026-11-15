import type { ChapterOutline, LessonOutline } from "./types";

export interface LessonNeighbours {
    previous?: LessonOutline;
    next?: LessonOutline;
}

export function neighboursOf(chapters: ChapterOutline[], lessonSlug: string): LessonNeighbours {
    const lessons = chapters.flatMap((chapter) => chapter.lessons);
    const index = lessons.findIndex((lesson) => lesson.slug === lessonSlug);
    if (index === -1) {
        return {};
    }
    return { previous: lessons[index - 1], next: lessons[index + 1] };
}
