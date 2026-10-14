"use client";

import { UserX } from "lucide-react";
import Link from "next/link";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { useAdminUser } from "@/features/admin/hooks/useAdminUser";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { UserPageContent } from "./UserPageContent";

const NOT_FOUND_STATUS = 404;

interface AdminUserPageProps {
    username: string;
}

export function AdminUserPage({ username }: AdminUserPageProps) {
    const { data: user, error, isPending, refetch } = useAdminUser(username);
    const isMissing = error instanceof ApiRequestError && error.status === NOT_FOUND_STATUS;

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <nav aria-label="Đường dẫn" className="text-[13px] text-muted-foreground">
                <Link href={ROUTES.adminUsers} className="hover:text-foreground">
                    Người dùng
                </Link>
                {` / ${username}`}
            </nav>
            {isPending ? (
                <div
                    role="status"
                    aria-busy="true"
                    aria-label="Đang tải thông tin người dùng"
                    className="flex flex-col gap-4"
                >
                    <Skeleton className="h-28" />
                    <Skeleton className="h-16 max-w-md" />
                    <Skeleton className="h-48" />
                </div>
            ) : null}
            {user ? <UserPageContent user={user} /> : null}
            {isMissing ? (
                <EmptyState
                    title="Không tìm thấy người dùng"
                    description="Tài khoản không tồn tại hoặc đường dẫn bị sai."
                    icon={<UserX />}
                    action={
                        <Button asChild size="sm" variant="outline">
                            <Link href={ROUTES.adminUsers}>Về danh sách người dùng</Link>
                        </Button>
                    }
                />
            ) : null}
            {!user && error && !isMissing ? (
                <ErrorState
                    title="Chưa tải được thông tin người dùng"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
        </main>
    );
}
