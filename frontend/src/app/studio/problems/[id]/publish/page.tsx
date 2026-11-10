import type { Metadata } from "next";

import { ProblemStudio } from "@/features/studio/components/ProblemStudio";

export const metadata: Metadata = {
    title: "Xem trước và xuất bản",
};

export default async function ProblemPublishPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    return <ProblemStudio problemId={id} step="publish" />;
}
