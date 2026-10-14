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

interface UserDetailPanelProps {
    username: string;
    onClose: () => void;
}

export function UserDetailPanel({ username, onClose }: UserDetailPanelProps) {
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
            <div
                aria-hidden="true"
                onClick={onClose}
                className="fixed inset-0 z-40 bg-black/50 lg:hidden"
            />
            <aside
                aria-label="Chi tiết người dùng"
                onKeyDown={(event) => {
                    if (event.key === "Escape") {
                        onClose();
                    }
                }}
                className="fixed inset-y-0 right-0 z-40 flex w-85 max-w-full flex-col gap-4 overflow-y-auto border-l bg-card p-5 lg:relative lg:inset-auto lg:z-auto lg:shrink-0 lg:overflow-visible lg:rounded-lg lg:border"
            >
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
            </aside>
        </>
    );
}
