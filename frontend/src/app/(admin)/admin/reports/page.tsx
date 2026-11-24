import type { Metadata } from "next";
import { Suspense } from "react";

import { AdminReports } from "@/features/admin/components/reports/AdminReports";

export const metadata: Metadata = {
    title: "Báo cáo vi phạm",
};

export default function AdminReportsPage() {
    return (
        <Suspense>
            <AdminReports />
        </Suspense>
    );
}
