import type { Metadata } from "next";

import { ProblemStudio } from "@/features/studio/components/ProblemStudio";

export const metadata: Metadata = {
    title: "Đáp án và testcase",
};

export default async function ProblemAnswerPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    return <ProblemStudio problemId={id} step="answer" />;
}
