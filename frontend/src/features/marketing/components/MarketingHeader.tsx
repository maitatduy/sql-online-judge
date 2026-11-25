import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { ROUTES } from "@/lib/routes";

import { MarketingAccount } from "./MarketingAccount";
import { MarketingNav } from "./MarketingNav";

export function MarketingHeader() {
    return (
        <header className="mx-auto flex max-w-7xl items-center gap-x-4 px-4 py-4 sm:px-6 lg:gap-x-6">
            <Link href={ROUTES.home} className="inline-flex min-h-11 items-center">
                <Logo />
            </Link>
            <MarketingNav />
            <div className="ml-auto flex items-center gap-2">
                <MarketingAccount />
            </div>
        </header>
    );
}
