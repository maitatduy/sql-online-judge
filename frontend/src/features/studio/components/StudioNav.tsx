"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

const ITEMS = [
    { href: ROUTES.studioProblems, label: "Bài của tôi" },
    { href: ROUTES.studioDatasets, label: "Dataset của tôi" },
];

export function StudioNav() {
    const pathname = usePathname();

    return (
        <nav aria-label="Khu vực người ra đề" className="border-b bg-card">
            <ul className="mx-auto flex max-w-[1280px] gap-1 px-6">
                {ITEMS.map(({ href, label }) => {
                    const isCurrent = pathname === href || pathname.startsWith(`${href}/`);
                    return (
                        <li key={href}>
                            <Link
                                href={href}
                                aria-current={isCurrent ? "page" : undefined}
                                className={cn(
                                    "-mb-px flex min-h-11 items-center border-b-2 px-3 text-sm font-semibold",
                                    isCurrent
                                        ? "border-brand text-brand"
                                        : "border-transparent text-muted-foreground hover:text-foreground",
                                )}
                            >
                                {label}
                            </Link>
                        </li>
                    );
                })}
            </ul>
        </nav>
    );
}
