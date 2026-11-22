import type { Metadata } from "next";

import { ContestCatalogue } from "@/features/contest/components/ContestCatalogue";

export const metadata: Metadata = {
    title: "Contest SQL",
    description: "Thi giải SQL theo luật ICPC, bảng xếp hạng trực tiếp và rating.",
};

export default function ContestsPage() {
    return <ContestCatalogue />;
}
