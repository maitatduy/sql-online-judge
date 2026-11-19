import type { Metadata } from "next";

import { LessonEditor } from "@/features/course-studio/components/LessonEditor";

export const metadata: Metadata = {
    title: "Soạn bài giảng",
};

export default async function LessonEditorPage({
    params,
}: {
    params: Promise<{ id: string; lessonId: string }>;
}) {
    const { id, lessonId } = await params;
    return <LessonEditor courseId={id} lessonId={lessonId} />;
}
