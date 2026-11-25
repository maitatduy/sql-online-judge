import { ADMIN_ROUTES } from "@/lib/admin-navigation";
import type { Role } from "@/lib/current-user";
import { ROUTES } from "@/lib/routes";

export interface MarketingLink {
    label: string;
    href: string;
}

const PUBLIC_LINKS: MarketingLink[] = [
    { label: "Học", href: ROUTES.learn },
    { label: "Luyện tập", href: ROUTES.problems },
    { label: "Contest", href: ROUTES.contests },
    { label: "Cộng đồng", href: ROUTES.community },
];

const STUDIO_LINKS: MarketingLink[] = [
    { label: "Ra đề", href: ROUTES.studioProblems },
    { label: "Tạo khoá học", href: ROUTES.studioCourses },
];

const ROLE_LINKS: Record<Role, MarketingLink[]> = {
    LEARNER: [],
    SETTER: STUDIO_LINKS,
    MODERATOR: [{ label: "Báo cáo vi phạm", href: ADMIN_ROUTES.reports }],
    ADMIN: [...STUDIO_LINKS, { label: "Quản trị", href: ROUTES.admin }],
};

export function marketingLinksFor(role: Role | undefined): MarketingLink[] {
    return role ? [...PUBLIC_LINKS, ...ROLE_LINKS[role]] : PUBLIC_LINKS;
}
