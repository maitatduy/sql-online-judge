import type { Metadata } from "next";

import { ProblemStudio } from "@/features/studio/components/ProblemStudio";

export const metadata: Metadata = {
    title: "Chọn database cho bài",
};

export default async function ProblemDatabasePage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    return <ProblemStudio problemId={id} step="database" />;
}
