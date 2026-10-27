import type { Metadata } from "next";

import { Workspace } from "@/features/practice/components/Workspace";

export const metadata: Metadata = {
    title: "Làm bài",
};

interface WorkspacePageProps {
    params: Promise<{ slug: string }>;
}

export default async function WorkspacePage({ params }: WorkspacePageProps) {
    const { slug } = await params;
    return <Workspace slug={slug} />;
}
