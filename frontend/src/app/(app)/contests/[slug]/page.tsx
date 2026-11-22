import type { Metadata } from "next";

import { ContestLobby } from "@/features/contest/components/ContestLobby";

interface ContestPageProps {
    params: Promise<{ slug: string }>;
}

export const metadata: Metadata = {
    title: "Contest SQL",
};

export default async function ContestPage({ params }: ContestPageProps) {
    const { slug } = await params;
    return <ContestLobby slug={decodeURIComponent(slug)} />;
}
