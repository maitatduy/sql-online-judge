import { Bell, Sparkles } from "lucide-react";
import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { TopBarAccount } from "@/components/shared/TopBarAccount";
import { TopBarNav } from "@/components/shared/TopBarNav";
import { TopBarSearch } from "@/components/shared/TopBarSearch";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

export function TopBar() {
    return (
        <header className="flex min-h-14 flex-wrap items-center gap-x-5 border-b bg-background px-4 md:flex-nowrap md:px-6">
            <Link href={ROUTES.home} className="inline-flex min-h-11 shrink-0 items-center">
                <Logo />
            </Link>
            <TopBarNav />
            <div className="ml-auto flex shrink-0 items-center gap-2">
                <TopBarSearch />
                <Button type="button" variant="ghost" size="icon" aria-label="Thông báo">
                    <Bell aria-hidden="true" />
                </Button>
                <Button asChild size="sm" className="bg-ai-soft text-ai hover:bg-ai-soft/80">
                    <Link href={ROUTES.ai}>
                        <Sparkles aria-hidden="true" />
                        <span className="sr-only sm:not-sr-only">Trợ lý AI</span>
                    </Link>
                </Button>
                <TopBarAccount />
            </div>
        </header>
    );
}
