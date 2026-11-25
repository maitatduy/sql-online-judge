import type { Metadata } from "next";
import { Suspense } from "react";

import { AdminSubmissions } from "@/features/admin/components/submissions/AdminSubmissions";

export const metadata: Metadata = {
    title: "Bài nộp",
};

export default function AdminSubmissionsPage() {
    return (
        <Suspense>
            <AdminSubmissions />
        </Suspense>
    );
}
