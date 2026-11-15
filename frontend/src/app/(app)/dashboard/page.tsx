import type { Metadata } from "next";

import { LearningDashboard } from "@/features/learning/components/dashboard/LearningDashboard";

export const metadata: Metadata = {
    title: "Bảng học tập",
};

export default function DashboardPage() {
    return <LearningDashboard />;
}
