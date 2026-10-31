import type { Metadata } from "next";

import { DatasetStudio } from "@/features/studio/components/DatasetStudio";

export const metadata: Metadata = {
    title: "Soạn dataset",
};

export default async function DatasetStudioPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    return <DatasetStudio datasetId={id} />;
}
