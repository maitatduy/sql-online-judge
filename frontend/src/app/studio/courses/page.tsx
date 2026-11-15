import type { Metadata } from "next";

import { MyCourses } from "@/features/course-studio/components/MyCourses";

export const metadata: Metadata = {
    title: "Khoá học của tôi",
};

export default function MyCoursesPage() {
    return <MyCourses />;
}
