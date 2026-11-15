import { BookOpenCheck, Flame, GraduationCap } from "lucide-react";

import { formatNumber } from "@/lib/format";

interface DashboardStatsProps {
    streakDays: number;
    lessonCount: number;
    courseCount: number;
}

export function DashboardStats({ streakDays, lessonCount, courseCount }: DashboardStatsProps) {
    const stats = [
        {
            label: "Chuỗi ngày",
            value: `${formatNumber(streakDays)} ngày`,
            icon: Flame,
            iconClass: "bg-streak/12 text-streak",
        },
        {
            label: "Bài đã học",
            value: `${formatNumber(lessonCount)} bài`,
            icon: BookOpenCheck,
            iconClass: "bg-brand/12 text-brand",
        },
        {
            label: "Khoá đang học",
            value: `${formatNumber(courseCount)} khoá`,
            icon: GraduationCap,
            iconClass: "bg-brand/12 text-brand",
        },
    ];

    return (
        <ul aria-label="Thống kê học" className="grid gap-4 sm:grid-cols-3">
            {stats.map(({ label, value, icon: Icon, iconClass }) => (
                <li
                    key={label}
                    className="flex items-center gap-4 rounded-lg border bg-card px-5 py-4"
                >
                    <span
                        aria-hidden="true"
                        className={`flex size-11 shrink-0 items-center justify-center rounded-full ${iconClass}`}
                    >
                        <Icon className="size-5" />
                    </span>
                    <div className="flex flex-col">
                        <span className="text-sm text-muted-foreground">{label}</span>
                        <span className="text-2xl font-extrabold tracking-tight">{value}</span>
                    </div>
                </li>
            ))}
        </ul>
    );
}
