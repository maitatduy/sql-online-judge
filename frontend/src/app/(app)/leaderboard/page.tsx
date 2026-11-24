import type { Metadata } from "next";
import { Suspense } from "react";

import { Leaderboard } from "@/features/leaderboard/components/Leaderboard";

export const metadata: Metadata = {
    title: "Bảng xếp hạng",
    description: "Xếp hạng rating của người thi contest SQL tính rating trên SQL Judge.",
};

export default function LeaderboardPage() {
    return (
        <Suspense>
            <Leaderboard />
        </Suspense>
    );
}
