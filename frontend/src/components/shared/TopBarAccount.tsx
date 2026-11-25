"use client";

import { Flame } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

import { AccountMenu } from "@/components/shared/AccountMenu";
import { Button } from "@/components/ui/button";
import { isUnauthenticatedError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { useLearningStreak } from "@/lib/useLearningStreak";

export function TopBarAccount() {
    const pathname = usePathname();
    const { data: currentUser, error, isPending } = useCurrentUser();
    const streakDays = useLearningStreak(currentUser !== undefined);

    if (isPending) {
        return <span aria-hidden="true" className="size-8 shrink-0 rounded-full bg-surface-2" />;
    }

    if (!currentUser) {
        return currentUser === null || isUnauthenticatedError(error) ? (
            <>
                <Button asChild variant="ghost">
                    <Link
                        href={
                            pathname === ROUTES.home ? ROUTES.login : ROUTES.loginWithNext(pathname)
                        }
                    >
                        Đăng nhập
                    </Link>
                </Button>
                <Button asChild className="hidden sm:inline-flex">
                    <Link href={ROUTES.register}>Bắt đầu miễn phí</Link>
                </Button>
            </>
        ) : null;
    }

    return (
        <>
            <Link
                href={ROUTES.dashboard}
                title="Chuỗi ngày học"
                className="hidden min-h-11 items-center gap-1.5 rounded-md px-2.5 text-lg font-bold text-streak hover:bg-streak/12 sm:inline-flex"
            >
                <Flame aria-hidden="true" className="size-5.5" />
                {formatNumber(streakDays ?? 0)}
                <span className="sr-only"> ngày học liên tiếp</span>
            </Link>
            <AccountMenu user={currentUser} />
        </>
    );
}
