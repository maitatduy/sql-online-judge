import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ROUTES } from "@/lib/routes";

interface AuthStandaloneShellProps {
    headerAside: React.ReactNode;
    children: React.ReactNode;
}

export function AuthStandaloneShell({ headerAside, children }: AuthStandaloneShellProps) {
    return (
        <>
            <header className="mx-auto flex w-full max-w-[1240px] items-center justify-between px-6 py-6 md:px-8">
                <Link href={ROUTES.home} className="inline-flex min-h-11 items-center">
                    <Logo />
                </Link>
                {headerAside}
            </header>
            <main
                id={MAIN_CONTENT_ID}
                className="mx-auto flex w-full max-w-[480px] flex-1 items-center px-6 pt-4 pb-16"
            >
                <div className="w-full">{children}</div>
            </main>
        </>
    );
}
