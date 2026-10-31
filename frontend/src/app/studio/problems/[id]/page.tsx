import type { Metadata } from "next";

import { ProblemStudio } from "@/features/studio/components/ProblemStudio";

export const metadata: Metadata = {
    title: "Soạn bài",
};

export default async function ProblemStudioPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    return <ProblemStudio problemId={id} />;
}
