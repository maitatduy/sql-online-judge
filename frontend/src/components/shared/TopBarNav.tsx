"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

const NAV_LINKS = [
    { label: "Học", href: ROUTES.learn },
    { label: "Luyện tập", href: ROUTES.problems },
    { label: "Contest", href: ROUTES.contests },
    { label: "Cộng đồng", href: ROUTES.community },
    { label: "Xếp hạng", href: ROUTES.leaderboard },
];

function isSectionOpen(pathname: string, sectionPath: string): boolean {
    return pathname === sectionPath || pathname.startsWith(`${sectionPath}/`);
}

export function TopBarNav() {
    const pathname = usePathname();

    return (
        <nav
            aria-label="Điều hướng chính"
            className="order-last flex w-full min-w-0 items-center overflow-x-auto md:order-none md:w-auto"
        >
            {NAV_LINKS.map((link) => {
                const isOpen = isSectionOpen(pathname, link.href);
                return (
                    <Link
                        key={link.href}
                        href={link.href}
                        aria-current={isOpen ? "page" : undefined}
                        className={cn(
                            "inline-flex h-11 shrink-0 items-center border-b-2 border-transparent px-3 font-medium whitespace-nowrap text-muted-foreground hover:text-foreground md:h-14",
                            isOpen && "border-brand font-semibold text-foreground",
                        )}
                    >
                        {link.label}
                    </Link>
                );
            })}
        </nav>
    );
}
