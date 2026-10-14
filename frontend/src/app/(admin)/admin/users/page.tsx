import type { Metadata } from "next";
import { Suspense } from "react";

import { AdminUsers } from "@/features/admin/components/users/AdminUsers";

export const metadata: Metadata = {
    title: "Người dùng",
};

export default function AdminUsersPage() {
    return (
        <Suspense>
            <AdminUsers />
        </Suspense>
    );
}
