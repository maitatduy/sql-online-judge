import type { Metadata } from "next";

import { LessonReader } from "@/features/learning/components/LessonReader";

interface LessonPageProps {
    params: Promise<{ slug: string; lessonSlug: string }>;
}

export const metadata: Metadata = {
    title: "Bài giảng SQL",
};

export default async function LessonPage({ params }: LessonPageProps) {
    const { slug, lessonSlug } = await params;
    return (
        <LessonReader
            courseSlug={decodeURIComponent(slug)}
            lessonSlug={decodeURIComponent(lessonSlug)}
        />
    );
}
