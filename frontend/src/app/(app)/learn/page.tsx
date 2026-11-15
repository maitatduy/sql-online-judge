import type { Metadata } from "next";
import { Suspense } from "react";

import { CourseCatalogue } from "@/features/learning/components/CourseCatalogue";

export const metadata: Metadata = {
    title: "Khoá học SQL",
    description: "Khoá học SQL từ cơ bản đến nâng cao, chạy truy vấn ngay trong bài giảng.",
};

export default function LearnPage() {
    return (
        <Suspense>
            <CourseCatalogue />
        </Suspense>
    );
}
