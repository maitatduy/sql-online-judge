import type { Metadata } from "next";

import { AdminContests } from "@/features/admin/components/contests/AdminContests";

export const metadata: Metadata = {
    title: "Contest",
};

export default function AdminContestsPage() {
    return <AdminContests />;
}
