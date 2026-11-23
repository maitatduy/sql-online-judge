import type { Metadata } from "next";
import { Suspense } from "react";

import { CommunityHome } from "@/features/community/components/CommunityHome";

export const metadata: Metadata = {
    title: "Cộng đồng",
    description: "Hỏi đáp, chia sẻ lời giải và kinh nghiệm phỏng vấn SQL cùng cộng đồng SQL Judge.",
};

export default function CommunityPage() {
    return (
        <Suspense>
            <CommunityHome />
        </Suspense>
    );
}
