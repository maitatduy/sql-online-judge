import { ChevronLeft } from "lucide-react";
import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ROUTES } from "@/lib/routes";

import type { AuthMode } from "../types";

import { AuthCard } from "./AuthCard";
import { AuthIntro } from "./AuthIntro";

interface AuthShellProps {
    mode: AuthMode;
    nextPath: string;
    children?: React.ReactNode;
}

export function AuthShell({ mode, nextPath, children }: AuthShellProps) {
    return (
        <>
            <header className="mx-auto flex w-full max-w-[1240px] items-center justify-between px-6 py-6 md:px-8">
                <Link href={ROUTES.home} className="inline-flex min-h-11 items-center">
                    <Logo />
                </Link>
                <Link
                    href={ROUTES.home}
                    className="inline-flex min-h-11 items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
                >
                    <ChevronLeft aria-hidden="true" className="size-4" />
                    Về trang chủ
                </Link>
            </header>
            <div className="mx-auto flex w-full max-w-[1240px] flex-1 flex-col items-center justify-center gap-8 px-6 pt-2 pb-12 md:px-8 lg:grid lg:grid-cols-[minmax(0,1fr)_460px] lg:content-center lg:items-start lg:gap-14">
                <AuthIntro mode={mode} />
                <main id={MAIN_CONTENT_ID} className="w-full max-w-[460px] min-w-0">
                    <AuthCard mode={mode} nextPath={nextPath}>
                        {children}
                    </AuthCard>
                </main>
            </div>
        </>
    );
}
