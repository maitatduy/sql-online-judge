"use client";

import { Flame } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

import { Avatar } from "@/components/shared/Avatar";
import { Button } from "@/components/ui/button";
import { isUnauthenticatedError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

export function TopBarAccount() {
    const pathname = usePathname();
    const { data: currentUser, error, isPending } = useCurrentUser();

    if (isPending) {
        return <span aria-hidden="true" className="size-8 shrink-0 rounded-full bg-surface-2" />;
    }

    if (!currentUser) {
        return isUnauthenticatedError(error) ? (
            <Button asChild variant="outline">
                <Link href={ROUTES.loginWithNext(pathname)}>Đăng nhập</Link>
            </Button>
        ) : null;
    }

    return (
        <>
            <span
                title="Chuỗi ngày học"
                className="hidden items-center gap-1 font-bold text-streak sm:inline-flex"
            >
                <Flame aria-hidden="true" className="size-4" />
                {formatNumber(currentUser.currentStreakDays)}
                <span className="sr-only"> ngày học liên tiếp</span>
            </span>
            <Link
                href={ROUTES.profile(currentUser.username)}
                aria-label="Hồ sơ của tôi"
                className="inline-flex min-h-11 min-w-11 items-center justify-center rounded-full"
            >
                <Avatar name={currentUser.displayName} />
            </Link>
        </>
    );
}
