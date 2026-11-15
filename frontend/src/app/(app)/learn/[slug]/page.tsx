import type { Metadata } from "next";

import { CourseOverview } from "@/features/learning/components/CourseOverview";

interface CoursePageProps {
    params: Promise<{ slug: string }>;
}

export const metadata: Metadata = {
    title: "Khoá học SQL",
};

export default async function CoursePage({ params }: CoursePageProps) {
    const { slug } = await params;
    return <CourseOverview slug={decodeURIComponent(slug)} />;
}
