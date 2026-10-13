"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";

import { ErrorState } from "@/components/shared/ErrorState";
import { ForbiddenPage } from "@/components/shared/ForbiddenPage";
import { Skeleton } from "@/components/shared/Skeleton";
import { TopBar } from "@/components/shared/TopBar";
import { ApiRequestError, isUnauthenticatedError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { AdminFrame } from "./AdminFrame";

interface AdminShellProps {
    children: React.ReactNode;
}

function AccessCheckSkeleton() {
    return (
        <div
            role="status"
            aria-busy="true"
            aria-label="Đang kiểm tra quyền truy cập"
            className="mx-auto flex max-w-xl flex-col gap-3 px-6 py-24"
        >
            <Skeleton className="h-6 w-2/5" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-4/5" />
        </div>
    );
}

export function AdminShell({ children }: AdminShellProps) {
    const pathname = usePathname();
    const router = useRouter();
    const { data: currentUser, error, isPending, refetch } = useCurrentUser();
    const isUnauthenticated = isUnauthenticatedError(error);

    useEffect(() => {
        if (isUnauthenticated) {
            router.replace(ROUTES.loginWithNext(pathname));
        }
    }, [isUnauthenticated, pathname, router]);

    if (isPending || isUnauthenticated) {
        return <AccessCheckSkeleton />;
    }

    if (!currentUser) {
        return (
            <div className="mx-auto max-w-xl px-6 py-24">
                <ErrorState
                    title="Chưa kiểm tra được quyền truy cập"
                    message="Trang quản trị chưa tải được. Bạn thử lại sau ít phút nhé."
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            </div>
        );
    }

    if (currentUser.role !== "ADMIN") {
        return (
            <>
                <TopBar />
                <ForbiddenPage
                    username={currentUser.username}
                    requiredRole="ADMIN"
                    description="Trang này dành cho ban quản trị."
                />
            </>
        );
    }

    return <AdminFrame user={currentUser}>{children}</AdminFrame>;
}
