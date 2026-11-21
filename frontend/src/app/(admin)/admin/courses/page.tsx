import type { Metadata } from "next";

import { AdminCourses } from "@/features/admin/components/courses/AdminCourses";

export const metadata: Metadata = {
    title: "Khoá học",
};

export default function AdminCoursesPage() {
    return <AdminCourses />;
}
