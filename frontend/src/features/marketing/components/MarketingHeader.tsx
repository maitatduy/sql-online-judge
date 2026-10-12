import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

const NAV_LINKS = [
    { label: "Lộ trình", href: ROUTES.learn },
    { label: "Bài tập", href: ROUTES.problems },
    { label: "Contest", href: ROUTES.contests },
    { label: "Cộng đồng", href: ROUTES.community },
];

export function MarketingHeader() {
    return (
        <header className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-6 gap-y-2 px-6 py-4.5">
            <Link href={ROUTES.home} className="inline-flex min-h-11 items-center">
                <Logo />
            </Link>
            <nav aria-label="Giới thiệu" className="hidden gap-1 md:flex">
                {NAV_LINKS.map((link) => (
                    <Button key={link.href} asChild variant="ghost">
                        <Link href={link.href}>{link.label}</Link>
                    </Button>
                ))}
            </nav>
            <div className="ml-auto flex items-center gap-2">
                <Button asChild variant="ghost">
                    <Link href={ROUTES.login}>Đăng nhập</Link>
                </Button>
                <Button asChild className="hidden sm:inline-flex">
                    <Link href={ROUTES.register}>Bắt đầu miễn phí</Link>
                </Button>
            </div>
        </header>
    );
}
