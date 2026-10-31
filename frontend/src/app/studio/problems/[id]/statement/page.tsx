import type { Metadata } from "next";

import { ProblemStudio } from "@/features/studio/components/ProblemStudio";

export const metadata: Metadata = {
    title: "Soạn đề bài",
};

export default async function ProblemStatementPage({
    params,
}: {
    params: Promise<{ id: string }>;
}) {
    const { id } = await params;
    return <ProblemStudio problemId={id} step="statement" />;
}
