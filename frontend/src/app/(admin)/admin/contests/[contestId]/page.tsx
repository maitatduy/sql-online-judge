import type { Metadata } from "next";

import { AdminContestDetail } from "@/features/admin/components/contests/AdminContestDetail";

interface AdminContestRouteProps {
    params: Promise<{ contestId: string }>;
}

export const metadata: Metadata = {
    title: "Contest",
};

export default async function AdminContestRoute({ params }: AdminContestRouteProps) {
    const { contestId } = await params;
    return <AdminContestDetail contestId={contestId} />;
}
