"use client";

import { Search } from "lucide-react";
import dynamic from "next/dynamic";
import { useMemo, useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { StaleDataNotice } from "@/features/admin/components/StaleDataNotice";
import { useAdminUsers } from "@/features/admin/hooks/useAdminUsers";
import { useUserSelection } from "@/features/admin/hooks/useUserSelection";
import { useUsersFilters } from "@/features/admin/hooks/useUsersFilters";
import { usersHref } from "@/features/admin/users/search-params";
import { ApiRequestError } from "@/lib/api-client";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { BulkActionBar } from "./BulkActionBar";
import { UserFilters } from "./UserFilters";
import type { UserRowHandlers } from "./UserRowActions";
import { UserSummaryTiles } from "./UserSummaryTiles";
import { UsersHeader } from "./UsersHeader";
import { UsersTable } from "./UsersTable";
import { UsersTableSkeleton } from "./UsersTableSkeleton";

const DeleteUserDialog = dynamic(
    () => import("./DeleteUserDialog").then((dialogModule) => dialogModule.DeleteUserDialog),
    { ssr: false },
);
const UserDetailDialog = dynamic(
    () => import("./UserDetailDialog").then((dialogModule) => dialogModule.UserDetailDialog),
    { ssr: false },
);
const RestoreUserDialog = dynamic(
    () => import("./RestoreUserDialog").then((dialogModule) => dialogModule.RestoreUserDialog),
    { ssr: false },
);
const EditUserDialog = dynamic(
    () => import("./EditUserDialog").then((dialogModule) => dialogModule.EditUserDialog),
    { ssr: false },
);

interface RowDialog {
    kind: "edit" | "delete" | "restore";
    username: string;
}

export function AdminUsers() {
    const { filters, changeFilters, clearFilters, openUser, closeUser } = useUsersFilters();
    const {
        data: users,
        dataUpdatedAt,
        error,
        isPending,
        isRefetchError,
        refetch,
    } = useAdminUsers(filters);
    const selection = useUserSelection();
    const { data: currentUser } = useCurrentUser();
    const [rowDialog, setRowDialog] = useState<RowDialog | null>(null);
    const closeRowDialog = () => setRowDialog(null);
    const rowHandlers = useMemo<UserRowHandlers>(
        () => ({
            onView: openUser,
            onEdit: (username) => setRowDialog({ kind: "edit", username }),
            onDelete: (username) => setRowDialog({ kind: "delete", username }),
            onRestore: (username) => setRowDialog({ kind: "restore", username }),
        }),
        [openUser],
    );
    const finishDelete = (username: string) => {
        selection.removeUser(username);
        setRowDialog(null);
    };
    const selectedUsernames = [...selection.selected];
    const hasUsers = users ? users.items.length > 0 : false;

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <UsersHeader
                filters={filters}
                filteredCount={users?.totalItems ?? null}
                selectedUsernames={selectedUsernames}
            />
            <UserSummaryTiles filters={filters} onChange={changeFilters} />
            {isRefetchError ? <StaleDataNotice updatedAtMs={dataUpdatedAt} /> : null}
            <section
                aria-label="Danh sách người dùng"
                className="overflow-hidden rounded-lg border bg-card"
            >
                <UserFilters filters={filters} onChange={changeFilters} onClear={clearFilters} />
                {selection.selected.size > 0 ? (
                    <BulkActionBar
                        selectedUsernames={selectedUsernames}
                        onClear={selection.clearSelection}
                    />
                ) : null}
                {isPending ? <UsersTableSkeleton /> : null}
                {!users && error ? (
                    <ErrorState
                        title="Chưa tải được danh sách người dùng"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={() => void refetch()}
                        className="border-0"
                    />
                ) : null}
                {users && hasUsers ? (
                    <>
                        <UsersTable
                            users={users.items}
                            selection={selection}
                            selfUsername={currentUser?.username ?? null}
                            rowHandlers={rowHandlers}
                        />
                        <Pagination
                            result={users}
                            hrefOf={(page) => usersHref({ ...filters, page })}
                        />
                    </>
                ) : null}
                {users && !hasUsers ? (
                    <EmptyState
                        title="Không tìm thấy người dùng"
                        description="Thử từ khoá khác hoặc bỏ bớt bộ lọc."
                        icon={<Search />}
                        action={
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={clearFilters}
                            >
                                Xoá lọc
                            </Button>
                        }
                        className="border-0"
                    />
                ) : null}
            </section>
            {filters.openUsername ? (
                <UserDetailDialog
                    key={filters.openUsername}
                    username={filters.openUsername}
                    onClose={closeUser}
                />
            ) : null}
            {rowDialog?.kind === "edit" ? (
                <EditUserDialog username={rowDialog.username} onClose={closeRowDialog} />
            ) : null}
            {rowDialog?.kind === "restore" ? (
                <RestoreUserDialog username={rowDialog.username} onClose={closeRowDialog} />
            ) : null}
            {rowDialog?.kind === "delete" ? (
                <DeleteUserDialog
                    username={rowDialog.username}
                    onClose={closeRowDialog}
                    onDeleted={finishDelete}
                />
            ) : null}
        </main>
    );
}
