import type { Metadata } from "next";

import { ContestStandings } from "@/features/contest/components/ContestStandings";

export const metadata: Metadata = {
    title: "Bảng xếp hạng contest",
};

interface ContestStandingsPageProps {
    params: Promise<{ slug: string }>;
    searchParams: Promise<{ page?: string }>;
}

function pageOf(value: string | undefined): number {
    const page = Number(value);
    return Number.isInteger(page) && page > 0 ? page : 1;
}

export default async function ContestStandingsPage({
    params,
    searchParams,
}: ContestStandingsPageProps) {
    const { slug } = await params;
    const { page } = await searchParams;
    return <ContestStandings slug={decodeURIComponent(slug)} page={pageOf(page)} />;
}
