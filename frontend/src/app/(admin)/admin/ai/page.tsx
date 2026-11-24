import type { Metadata } from "next";
import { Suspense } from "react";

import { AdminAi } from "@/features/admin/components/ai/AdminAi";

export const metadata: Metadata = {
    title: "Trợ lý AI",
};

export default function AdminAiPage() {
    return (
        <Suspense>
            <AdminAi />
        </Suspense>
    );
}
