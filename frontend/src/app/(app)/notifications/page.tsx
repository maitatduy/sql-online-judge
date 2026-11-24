import type { Metadata } from "next";

import { NotificationsPage } from "@/features/notifications/components/NotificationsPage";

export const metadata: Metadata = { title: "Thông báo" };

export default function NotificationsRoute() {
    return <NotificationsPage />;
}
