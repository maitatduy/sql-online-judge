import type { Metadata } from "next";
import { Suspense } from "react";

import { AdminProblemReviews } from "@/features/admin/components/problem-reviews/AdminProblemReviews";

export const metadata: Metadata = {
    title: "Duyệt bài tập",
};

export default function AdminProblemReviewsPage() {
    return (
        <Suspense>
            <AdminProblemReviews />
        </Suspense>
    );
}
