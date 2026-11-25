import Link from "next/link";

import { AiShortcut } from "@/components/shared/AiShortcut";
import { Logo } from "@/components/shared/Logo";
import { NotificationBell } from "@/components/shared/NotificationBell";
import { TopBarAccount } from "@/components/shared/TopBarAccount";
import { TopBarNav } from "@/components/shared/TopBarNav";
import { ROUTES } from "@/lib/routes";

export function TopBar() {
    return (
        <header className="mx-auto flex max-w-7xl items-center gap-x-4 px-4 py-4 sm:px-6 lg:gap-x-6">
            <Link href={ROUTES.home} className="inline-flex min-h-11 shrink-0 items-center">
                <Logo />
            </Link>
            <TopBarNav />
            <div className="ml-auto flex items-center gap-2">
                <AiShortcut />
                <NotificationBell />
                <TopBarAccount />
            </div>
        </header>
    );
}
