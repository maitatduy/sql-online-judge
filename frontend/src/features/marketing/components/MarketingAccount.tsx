"use client";

import Link from "next/link";

import { TopBarAccount } from "@/components/shared/TopBarAccount";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

export function MarketingAccount() {
    const { data: currentUser, isPending } = useCurrentUser();

    if (isPending) {
        return <span aria-hidden="true" className="h-10 w-48 rounded-md bg-surface-2" />;
    }

    if (currentUser) {
        return (
            <>
                <Button asChild className="hidden sm:inline-flex">
                    <Link href={ROUTES.problems}>Tiếp tục luyện tập</Link>
                </Button>
                <TopBarAccount />
            </>
        );
    }

    return (
        <>
            <Button asChild variant="ghost">
                <Link href={ROUTES.login}>Đăng nhập</Link>
            </Button>
            <Button asChild className="hidden sm:inline-flex">
                <Link href={ROUTES.register}>Bắt đầu miễn phí</Link>
            </Button>
        </>
    );
}
