import type { Metadata } from "next";
import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { VerifyEmailNotice } from "@/features/auth/components/VerifyEmailNotice";
import { ROUTES } from "@/lib/routes";

export const metadata: Metadata = {
    title: "Xác thực email",
    robots: { index: false },
};

export default function VerifyEmailPage() {
    return (
        <>
            <header className="mx-auto flex w-full max-w-[1240px] items-center justify-between px-6 py-6 md:px-8">
                <Link href={ROUTES.home} className="inline-flex min-h-11 items-center">
                    <Logo />
                </Link>
                <span className="text-[13px] text-muted-foreground">Bước 2/3 · Xác thực email</span>
            </header>
            <main
                id={MAIN_CONTENT_ID}
                className="mx-auto flex w-full max-w-[480px] flex-1 items-center px-6 pt-4 pb-16"
            >
                <div className="w-full">
                    <VerifyEmailNotice />
                </div>
            </main>
        </>
    );
}
