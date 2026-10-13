import type { Metadata } from "next";

import { AdminOverview } from "@/features/admin/components/AdminOverview";

export const metadata: Metadata = {
    title: "Tổng quan",
};

export default function AdminOverviewPage() {
    return <AdminOverview />;
}
