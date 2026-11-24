import type { Metadata } from "next";
import { Suspense } from "react";

import { AiPage } from "@/features/ai/components/AiPage";

export const metadata: Metadata = { title: "Trợ lý AI" };

export default function AssistantPage() {
    return (
        <Suspense>
            <AiPage />
        </Suspense>
    );
}
