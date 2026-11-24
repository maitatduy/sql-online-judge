import type { Metadata } from "next";

import { AdminAnnouncements } from "@/features/admin/components/announcements/AdminAnnouncements";

export const metadata: Metadata = { title: "Thông báo hệ thống" };

export default function AdminMessagesPage() {
    return <AdminAnnouncements />;
}
