import type { Metadata } from "next";

import { AdminJudge } from "@/features/admin/components/judge/AdminJudge";

export const metadata: Metadata = {
    title: "Máy chấm",
};

export default function AdminJudgePage() {
    return <AdminJudge />;
}
