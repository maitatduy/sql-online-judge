import Link from "next/link";

import type { UserPageTab } from "@/features/admin/users/user-page-tab";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

const TAB_CLASS =
    "inline-flex h-10 items-center border-b-2 px-1 text-[13px] font-semibold whitespace-nowrap max-md:h-11";
const IDLE_CLASS = "border-transparent text-muted-foreground hover:text-foreground";
const ACTIVE_CLASS = "border-brand text-brand";

interface UserPageTabsProps {
    username: string;
    activeTab: UserPageTab;
    violationCount: number;
    onSelect: (tab: UserPageTab) => void;
}

export function UserPageTabs({ username, activeTab, violationCount, onSelect }: UserPageTabsProps) {
    const userQuery = `?user=${encodeURIComponent(username)}`;
    const tabButton = (tab: UserPageTab, label: string) => (
        <button
            type="button"
            aria-current={activeTab === tab ? "page" : undefined}
            onClick={() => onSelect(tab)}
            className={cn(TAB_CLASS, activeTab === tab ? ACTIVE_CLASS : IDLE_CLASS)}
        >
            {label}
        </button>
    );
    const tabLink = (href: string, label: string) => (
        <Link href={href} className={cn(TAB_CLASS, IDLE_CLASS)}>
            {label}
        </Link>
    );

    return (
        <nav aria-label="Mục chi tiết" className="flex gap-5 overflow-x-auto border-b">
            {tabButton("overview", "Tổng quan")}
            {tabLink(`${ROUTES.adminSubmissions}${userQuery}`, "Bài nộp")}
            {tabButton("contests", "Contest")}
            {tabButton("violations", `Vi phạm · ${violationCount}`)}
            {tabButton("sessions", "Phiên đăng nhập")}
            {tabLink(`${ROUTES.adminAuditLog}${userQuery}`, "Nhật ký")}
        </nav>
    );
}
