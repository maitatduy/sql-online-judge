import { X } from "lucide-react";
import { useEffect } from "react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { useAdminUser } from "@/features/admin/hooks/useAdminUser";
import { ApiRequestError } from "@/lib/api-client";

import { UserDetailContent } from "./UserDetailContent";

const NOT_FOUND_STATUS = 404;

interface UserDetailBodyProps {
    username: string;
    onClose: () => void;
}

export function UserDetailBody({ username, onClose }: UserDetailBodyProps) {
    const showToast = useToast();
    const { data: user, error, isPending, refetch } = useAdminUser(username);
    const isMissing = error instanceof ApiRequestError && error.status === NOT_FOUND_STATUS;

    useEffect(() => {
        if (isMissing) {
            showToast(`Không tìm thấy người dùng ${username}`, "error");
            onClose();
        }
    }, [isMissing, username, showToast, onClose]);

    return (
        <>
            <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                aria-label="Đóng"
                onClick={onClose}
                className="absolute top-4 right-4"
            >
                <X aria-hidden="true" />
            </Button>
            {isPending ? (
                <div
                    role="status"
                    aria-busy="true"
                    aria-label="Đang tải chi tiết người dùng"
                    className="flex flex-col gap-3 pr-9"
                >
                    <Skeleton className="size-13 rounded-full" />
                    <Skeleton className="h-4 w-2/3" />
                    <Skeleton className="h-14" />
                    <Skeleton className="h-24" />
                </div>
            ) : null}
            {user ? <UserDetailContent user={user} /> : null}
            {!user && error && !isMissing ? (
                <ErrorState
                    title="Chưa tải được chi tiết người dùng"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="mt-9 border-0 p-0"
                />
            ) : null}
        </>
    );
}
