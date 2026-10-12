import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { ROUTES } from "@/lib/routes";

const FOOTER_GROUPS = [
    {
        title: "Sản phẩm",
        links: [
            { label: "Lộ trình học", href: ROUTES.learn },
            { label: "Bài tập", href: ROUTES.problems },
            { label: "Contest", href: ROUTES.contests },
        ],
    },
    {
        title: "Cộng đồng",
        links: [
            { label: "Diễn đàn", href: ROUTES.community },
            { label: "Xếp hạng", href: ROUTES.leaderboard },
        ],
    },
];

export function MarketingFooter() {
    return (
        <footer className="border-t">
            <div className="mx-auto grid max-w-7xl grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-8 px-6 py-12">
                <div>
                    <Logo />
                    <p className="mt-3 text-[13px] text-muted-foreground">
                        Nền tảng học và thi SQL.
                    </p>
                </div>
                {FOOTER_GROUPS.map((group) => (
                    <nav key={group.title} aria-label={group.title}>
                        <h2 className="mb-2.5 font-bold">{group.title}</h2>
                        <ul className="flex flex-col md:gap-1.5">
                            {group.links.map((link) => (
                                <li key={link.href}>
                                    <Link
                                        href={link.href}
                                        className="inline-flex min-h-11 items-center text-brand underline-offset-4 hover:underline md:min-h-0"
                                    >
                                        {link.label}
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </nav>
                ))}
            </div>
        </footer>
    );
}
