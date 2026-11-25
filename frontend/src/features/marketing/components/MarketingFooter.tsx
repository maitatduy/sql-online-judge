import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { ROUTES } from "@/lib/routes";
import { SITE_NAME } from "@/lib/site";

import { ROADMAP_STAGES } from "../roadmap-stages";

interface FooterLink {
    label: string;
    href: string;
}

interface FooterGroup {
    title: string;
    links: FooterLink[];
}

const FOOTER_GROUPS: FooterGroup[] = [
    {
        title: "Học tập",
        links: [
            { label: "Khoá học", href: ROUTES.learn },
            { label: "Bài tập", href: ROUTES.problems },
            { label: "Trợ lý AI", href: ROUTES.ai },
        ],
    },
    {
        title: "Chủ đề",
        links: ROADMAP_STAGES.map((stage) => ({
            label: stage.title,
            href: `${ROUTES.problems}?tag=${encodeURIComponent(stage.tag)}`,
        })),
    },
    {
        title: "Thi đấu",
        links: [
            { label: "Contest", href: ROUTES.contests },
            { label: "Bảng xếp hạng", href: ROUTES.leaderboard },
        ],
    },
    {
        title: "Cộng đồng",
        links: [
            { label: "Bài viết", href: ROUTES.community },
            { label: "Viết bài mới", href: ROUTES.communityNew },
        ],
    },
    {
        title: "Tài khoản",
        links: [
            { label: "Đăng ký", href: ROUTES.register },
            { label: "Đăng nhập", href: ROUTES.login },
            { label: "Cài đặt", href: ROUTES.settings },
        ],
    },
];

const DIALECTS = "PostgreSQL, MySQL, SQLite";

export function MarketingFooter() {
    return (
        <footer className="border-t bg-surface">
            <div className="mx-auto grid max-w-7xl gap-10 px-6 pt-12 pb-8 lg:grid-cols-[1.4fr_5fr]">
                <div className="max-w-xs">
                    <Logo />
                    <p className="mt-3 text-muted-foreground">
                        Học SQL bằng cách viết SQL, có máy chấm tự động.
                    </p>
                </div>
                <div className="grid grid-cols-2 gap-8 sm:grid-cols-3 lg:grid-cols-5">
                    {FOOTER_GROUPS.map((group) => (
                        <nav key={group.title} aria-label={group.title}>
                            <h2 className="mb-3 text-sm font-bold tracking-[0.04em] uppercase">
                                {group.title}
                            </h2>
                            <ul className="flex flex-col">
                                {group.links.map((link) => (
                                    <li key={link.href}>
                                        <Link
                                            href={link.href}
                                            className="inline-flex min-h-11 items-center text-muted-foreground transition-colors hover:text-brand md:min-h-9"
                                        >
                                            {link.label}
                                        </Link>
                                    </li>
                                ))}
                            </ul>
                        </nav>
                    ))}
                </div>
            </div>
            <div className="border-t">
                <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-6 py-5 text-sm text-muted-foreground">
                    <p>{`© ${new Date().getFullYear()} ${SITE_NAME}`}</p>
                    <p>{`Chấm trên ${DIALECTS}`}</p>
                </div>
            </div>
        </footer>
    );
}
