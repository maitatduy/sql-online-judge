import { useId, useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { NativeSelect } from "@/components/shared/NativeSelect";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ActionDialog } from "@/features/admin/components/users/ActionDialog";
import { useRoleInvitations } from "@/features/admin/hooks/useRoleInvitations";
import { INVITATION_STATUS_LABELS } from "@/features/admin/invitations/status-labels";
import type { InvitationStatus } from "@/features/admin/invitations/types";
import { useDebouncedValue } from "@/lib/useDebouncedValue";

import { InvitationRow } from "./InvitationRow";

const SEARCH_DEBOUNCE_MS = 300;
const ALL_STATUSES = "ALL";
const STATUS_OPTIONS = Object.entries(INVITATION_STATUS_LABELS) as [InvitationStatus, string][];
const COLUMNS = ["Email", "Vai trò", "Trạng thái", "Người mời", "Ngày mời"];

interface InvitationsDialogProps {
    onClose: () => void;
}

export function InvitationsDialog({ onClose }: InvitationsDialogProps) {
    const statusId = useId();
    const [status, setStatus] = useState<InvitationStatus | null>("PENDING");
    const [search, setSearch] = useState("");
    const [page, setPage] = useState(1);
    const q = useDebouncedValue(search.trim(), SEARCH_DEBOUNCE_MS);
    const invitations = useRoleInvitations({ status, q, page });
    const result = invitations.data;

    function handleStatusChange(value: string) {
        setStatus(value === ALL_STATUSES ? null : (value as InvitationStatus));
        setPage(1);
    }

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    return (
        <ActionDialog
            title="Lời mời"
            description="Gửi lại lời mời hết hạn hoặc huỷ lời mời đang chờ."
            onClose={onClose}
            className="max-w-[760px]"
        >
            <div className="flex flex-wrap items-center gap-2">
                <label htmlFor={statusId} className="sr-only">
                    Trạng thái
                </label>
                <NativeSelect
                    id={statusId}
                    value={status ?? ALL_STATUSES}
                    onChange={(event) => handleStatusChange(event.target.value)}
                >
                    {STATUS_OPTIONS.map(([value, label]) => (
                        <option key={value} value={value}>
                            {label}
                        </option>
                    ))}
                    <option value={ALL_STATUSES}>Tất cả</option>
                </NativeSelect>
                <Input
                    type="search"
                    aria-label="Tìm theo email"
                    placeholder="Tìm theo email"
                    value={search}
                    onChange={(event) => handleSearchChange(event.target.value)}
                    className="h-9 min-w-0 flex-1"
                />
            </div>
            {invitations.isError && !result ? (
                <ErrorState
                    title="Chưa tải được danh sách lời mời"
                    message={invitations.error.message}
                    onRetry={() => void invitations.refetch()}
                />
            ) : !result ? (
                <div role="status" aria-label="Đang tải lời mời" className="flex flex-col gap-2">
                    <Skeleton className="h-9 w-full" />
                    <Skeleton className="h-9 w-full" />
                    <Skeleton className="h-9 w-full" />
                </div>
            ) : result.items.length === 0 ? (
                <EmptyState title="Không có lời mời nào khớp bộ lọc." />
            ) : (
                <div className="overflow-x-auto rounded-lg border">
                    <table className="w-full text-left text-[13px]">
                        <thead className="bg-surface-2 text-xs text-muted-foreground">
                            <tr>
                                {COLUMNS.map((column) => (
                                    <th
                                        key={column}
                                        scope="col"
                                        className="px-3 py-2 font-semibold"
                                    >
                                        {column}
                                    </th>
                                ))}
                                <th scope="col" className="px-3 py-2 text-right font-semibold">
                                    Hành động
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {result.items.map((invitation) => (
                                <InvitationRow key={invitation.id} invitation={invitation} />
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            {result && result.totalPages > 1 ? (
                <div className="flex items-center justify-between text-[13px]">
                    <span aria-live="polite" className="text-muted-foreground">
                        Trang {result.page}/{result.totalPages}
                    </span>
                    <div className="flex gap-1">
                        <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            disabled={page <= 1}
                            onClick={() => setPage(page - 1)}
                        >
                            Trước
                        </Button>
                        <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            disabled={page >= result.totalPages}
                            onClick={() => setPage(page + 1)}
                        >
                            Sau
                        </Button>
                    </div>
                </div>
            ) : null}
        </ActionDialog>
    );
}
