import type { Metadata } from "next";

import { CourseStudio } from "@/features/course-studio/components/CourseStudio";

export const metadata: Metadata = {
    title: "Soạn khoá học",
};

export default async function CourseStudioPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    return <CourseStudio courseId={id} />;
}
