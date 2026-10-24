import type { Metadata } from "next";
import { Suspense } from "react";

import { ProblemList } from "@/features/practice/components/ProblemList";

export const metadata: Metadata = {
    title: "Luyện tập SQL",
    description:
        "Danh sách bài tập SQL có máy chấm tự động, lọc theo độ khó và chủ đề: JOIN, GROUP BY, CTE, window function.",
};

export default function ProblemsPage() {
    return (
        <Suspense>
            <ProblemList />
        </Suspense>
    );
}
