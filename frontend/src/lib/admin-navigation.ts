import {
    BookOpen,
    ClipboardCheck,
    Code,
    Flag,
    History,
    LayoutDashboard,
    Lightbulb,
    ListChecks,
    type LucideIcon,
    Mail,
    Server,
    Sparkles,
    Trophy,
    Users,
} from "lucide-react";

import { ROUTES } from "@/lib/routes";

export interface AdminNavigationCounts {
    abuseReports: number;
    supportMessages: number;
    problemReviews: number;
    courseReviews: number;
    hasLiveContest: boolean;
    hasJudgeIncident: boolean;
}

export type AdminServiceCounts = Omit<AdminNavigationCounts, "problemReviews" | "courseReviews">;

export type AdminNavigationIndicator =
    | "abuseReports"
    | "supportMessages"
    | "problemReviews"
    | "courseReviews"
    | "liveContest"
    | "judgeIncident";

export interface AdminNavigationItem {
    label: string;
    href: string;
    icon: LucideIcon;
    indicator?: AdminNavigationIndicator;
}

export interface AdminNavigationGroup {
    title: string;
    items: AdminNavigationItem[];
}

export const ADMIN_ROUTES = {
    reports: `${ROUTES.admin}/reports`,
    messages: `${ROUTES.admin}/messages`,
    problemReviews: `${ROUTES.admin}/problem-reviews`,
    courseReviews: `${ROUTES.admin}/course-reviews`,
    contests: `${ROUTES.admin}/contests`,
    contest: (contestId: string) => `${ROUTES.admin}/contests/${encodeURIComponent(contestId)}`,
    judge: `${ROUTES.admin}/judge`,
    ai: `${ROUTES.admin}/ai`,
} as const;

export const ADMIN_OVERVIEW_ITEM: AdminNavigationItem = {
    label: "Tổng quan",
    href: ROUTES.admin,
    icon: LayoutDashboard,
};

export const ADMIN_NAVIGATION_GROUPS: AdminNavigationGroup[] = [
    {
        title: "Người & cộng đồng",
        items: [
            { label: "Người dùng", href: `${ROUTES.admin}/users`, icon: Users },
            {
                label: "Báo cáo vi phạm",
                href: ADMIN_ROUTES.reports,
                icon: Flag,
                indicator: "abuseReports",
            },
            {
                label: "Thông báo & tin nhắn",
                href: ADMIN_ROUTES.messages,
                icon: Mail,
                indicator: "supportMessages",
            },
        ],
    },
    {
        title: "Nội dung",
        items: [
            { label: "Kho bài tập", href: `${ROUTES.admin}/problems`, icon: Code },
            {
                label: "Duyệt bài tập",
                href: ADMIN_ROUTES.problemReviews,
                icon: ClipboardCheck,
                indicator: "problemReviews",
            },
            { label: "Lời giải chính thức", href: `${ROUTES.admin}/solutions`, icon: Lightbulb },
            {
                label: "Duyệt khoá học",
                href: ADMIN_ROUTES.courseReviews,
                icon: ClipboardCheck,
                indicator: "courseReviews",
            },
            { label: "Khoá học", href: `${ROUTES.admin}/courses`, icon: BookOpen },
            {
                label: "Contest",
                href: ADMIN_ROUTES.contests,
                icon: Trophy,
                indicator: "liveContest",
            },
        ],
    },
    {
        title: "Hệ thống",
        items: [
            { label: "Bài nộp", href: `${ROUTES.admin}/submissions`, icon: ListChecks },
            {
                label: "Máy chấm",
                href: ADMIN_ROUTES.judge,
                icon: Server,
                indicator: "judgeIncident",
            },
            { label: "Trợ lý AI", href: ADMIN_ROUTES.ai, icon: Sparkles },
            { label: "Nhật ký thao tác", href: `${ROUTES.admin}/audit-log`, icon: History },
        ],
    },
];

export function isAdminItemActive(activePath: string, item: AdminNavigationItem): boolean {
    if (item.href === ROUTES.admin) {
        return activePath === ROUTES.admin;
    }
    return activePath === item.href || activePath.startsWith(`${item.href}/`);
}
