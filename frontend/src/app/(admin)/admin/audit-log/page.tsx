import type { Metadata } from "next";
import { Suspense } from "react";

import { AdminAuditLog } from "@/features/admin/components/audit/AdminAuditLog";

export const metadata: Metadata = {
    title: "Nhật ký thao tác",
};

export default function AdminAuditLogPage() {
    return (
        <Suspense>
            <AdminAuditLog />
        </Suspense>
    );
}
