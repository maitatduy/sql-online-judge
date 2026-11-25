import { ADMIN_ROUTES } from "@/lib/admin-navigation";
import type { Role } from "@/lib/current-user";
import { ROUTES } from "@/lib/routes";

export interface SiteLink {
    label: string;
    href: string;
}

const DASHBOARD_LINK: SiteLink = { label: "Tổng quan", href: ROUTES.dashboard };

const PUBLIC_LINKS: SiteLink[] = [
    { label: "Học", href: ROUTES.learn },
    { label: "Luyện tập", href: ROUTES.problems },
    { label: "Contest", href: ROUTES.contests },
    { label: "Cộng đồng", href: ROUTES.community },
    { label: "Xếp hạng", href: ROUTES.leaderboard },
];

const STUDIO_LINKS: SiteLink[] = [
    { label: "Ra đề", href: ROUTES.studioProblems },
    { label: "Tạo khoá học", href: ROUTES.studioCourses },
];

const ROLE_LINKS: Record<Role, SiteLink[]> = {
    LEARNER: [],
    SETTER: STUDIO_LINKS,
    MODERATOR: [{ label: "Báo cáo vi phạm", href: ADMIN_ROUTES.reports }],
    ADMIN: [...STUDIO_LINKS, { label: "Quản trị", href: ROUTES.admin }],
};

export const ACCOUNT_MENU_LINKS: SiteLink[] = [
    { label: "Trợ lý AI", href: ROUTES.ai },
    { label: "Thông báo", href: ROUTES.notifications },
];

export function siteLinksFor(role: Role | undefined): SiteLink[] {
    return role ? [DASHBOARD_LINK, ...PUBLIC_LINKS] : PUBLIC_LINKS;
}

export function roleLinksFor(role: Role): SiteLink[] {
    return ROLE_LINKS[role];
}

export function isSectionOpen(pathname: string, sectionPath: string): boolean {
    return pathname === sectionPath || pathname.startsWith(`${sectionPath}/`);
}
