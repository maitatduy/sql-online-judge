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

export interface LessonPosition {
    chapterNumber: number;
    lessonNumber: number;
    lessonCount: number;
}

export function positionOf(
    chapters: ChapterOutline[],
    lessonSlug: string,
): LessonPosition | undefined {
    const lessons = chapters.flatMap((chapter, chapterIndex) =>
        chapter.lessons.map((lesson) => ({ slug: lesson.slug, chapterNumber: chapterIndex + 1 })),
    );
    const index = lessons.findIndex((lesson) => lesson.slug === lessonSlug);
    if (index === -1) {
        return undefined;
    }
    return {
        chapterNumber: lessons[index].chapterNumber,
        lessonNumber: index + 1,
        lessonCount: lessons.length,
    };
}
