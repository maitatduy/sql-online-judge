import {
    BookOpen,
    ClipboardCheck,
    Code,
    FileText,
    Flag,
    History,
    LayoutDashboard,
    Lightbulb,
    ListChecks,
    type LucideIcon,
    Mail,
    Server,
    Settings,
    Sparkles,
    Trophy,
    Users,
} from "lucide-react";

import { ROUTES } from "@/lib/routes";

export interface AdminNavigationCounts {
    abuseReports: number;
    supportMessages: number;
    problemReviews: number;
    hasLiveContest: boolean;
    hasJudgeIncident: boolean;
}

export type AdminNavigationIndicator =
    "abuseReports" | "supportMessages" | "problemReviews" | "liveContest" | "judgeIncident";

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
            { label: "Khoá học", href: `${ROUTES.admin}/courses`, icon: BookOpen },
            {
                label: "Contest",
                href: `${ROUTES.admin}/contests`,
                icon: Trophy,
                indicator: "liveContest",
            },
            { label: "Trang nội dung", href: `${ROUTES.admin}/pages`, icon: FileText },
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
            { label: "Cấu hình", href: `${ROUTES.admin}/settings`, icon: Settings },
        ],
    },
];

export function isAdminItemActive(activePath: string, item: AdminNavigationItem): boolean {
    if (item.href === ROUTES.admin) {
        return activePath === ROUTES.admin;
    }
    return activePath === item.href || activePath.startsWith(`${item.href}/`);
}
