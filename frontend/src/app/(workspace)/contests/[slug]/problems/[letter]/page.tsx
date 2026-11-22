import type { Metadata } from "next";

import { ContestWorkspace } from "@/features/practice/components/ContestWorkspace";

export const metadata: Metadata = {
    title: "Thi contest",
};

interface ContestWorkspacePageProps {
    params: Promise<{ slug: string; letter: string }>;
}

export default async function ContestWorkspacePage({ params }: ContestWorkspacePageProps) {
    const { slug, letter } = await params;
    return <ContestWorkspace slug={decodeURIComponent(slug)} letter={decodeURIComponent(letter)} />;
}
