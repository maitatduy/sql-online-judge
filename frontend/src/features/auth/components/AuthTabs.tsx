import Link from "next/link";

import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { AuthMode } from "../types";

interface AuthTabsProps {
    mode: AuthMode;
}

const TABS: { mode: AuthMode; label: string; href: string }[] = [
    { mode: "login", label: "Đăng nhập", href: ROUTES.login },
    { mode: "register", label: "Đăng ký", href: ROUTES.register },
];

export function AuthTabs({ mode }: AuthTabsProps) {
    return (
        <nav aria-label="Chọn hình thức" className="flex gap-1 rounded-lg bg-surface-2 p-1">
            {TABS.map((tab) => {
                const isActive = tab.mode === mode;
                return (
                    <Link
                        key={tab.mode}
                        href={tab.href}
                        aria-current={isActive ? "page" : undefined}
                        className={cn(
                            "inline-flex h-11 flex-1 items-center justify-center rounded-md text-sm font-semibold",
                            isActive
                                ? "bg-background text-foreground shadow-sm"
                                : "text-muted-foreground hover:text-foreground",
                        )}
                    >
                        {tab.label}
                    </Link>
                );
            })}
        </nav>
    );
}
