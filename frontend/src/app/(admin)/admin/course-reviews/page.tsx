import type { Metadata } from "next";

import { AdminCourseReviews } from "@/features/admin/components/course-reviews/AdminCourseReviews";

export const metadata: Metadata = {
    title: "Duyệt khoá học",
};

export default function AdminCourseReviewsPage() {
    return <AdminCourseReviews />;
}
