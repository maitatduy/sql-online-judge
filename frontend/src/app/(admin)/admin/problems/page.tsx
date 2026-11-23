import type { Metadata } from "next";
import { Suspense } from "react";

import { AdminProblemBank } from "@/features/admin/components/problem-bank/AdminProblemBank";

export const metadata: Metadata = {
    title: "Kho bài tập",
};

export default function AdminProblemBankPage() {
    return (
        <Suspense>
            <AdminProblemBank />
        </Suspense>
    );
}
