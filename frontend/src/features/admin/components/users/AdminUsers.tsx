"use client";

import { Search } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { StaleDataNotice } from "@/features/admin/components/StaleDataNotice";
import { useAdminUsers } from "@/features/admin/hooks/useAdminUsers";
import { useUserSelection } from "@/features/admin/hooks/useUserSelection";
import { useUsersFilters } from "@/features/admin/hooks/useUsersFilters";
import { ApiRequestError } from "@/lib/api-client";

import { BulkActionBar } from "./BulkActionBar";
import { UserDetailPanel } from "./UserDetailPanel";
import { UserFilters } from "./UserFilters";
import { UserSummaryTiles } from "./UserSummaryTiles";
import { UsersPagination } from "./UsersPagination";
import { UsersTable } from "./UsersTable";
import { UsersTableSkeleton } from "./UsersTableSkeleton";

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
    const hasUsers = users ? users.items.length > 0 : false;

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <div>
                <h1 className="text-[26px] font-extrabold">Người dùng</h1>
                <p className="mt-0.5 text-[13px] text-muted-foreground">
                    Quản lý tài khoản, vai trò và trạng thái
                </p>
            </div>
            <UserSummaryTiles filters={filters} onChange={changeFilters} />
            {isRefetchError ? <StaleDataNotice updatedAtMs={dataUpdatedAt} /> : null}
            <div className="flex items-start gap-4">
                <section
                    aria-label="Danh sách người dùng"
                    className="min-w-0 flex-1 overflow-hidden rounded-lg border bg-card"
                >
                    <UserFilters
                        filters={filters}
                        onChange={changeFilters}
                        onClear={clearFilters}
                    />
                    {selection.selected.size > 0 ? (
                        <BulkActionBar
                            selectedCount={selection.selected.size}
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
                                sort={filters.sort}
                                onSortChange={(sort) => changeFilters({ sort })}
                                openUsername={filters.openUsername}
                                onOpenUser={openUser}
                                selection={selection}
                            />
                            <UsersPagination filters={filters} result={users} />
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
                    <UserDetailPanel
                        key={filters.openUsername}
                        username={filters.openUsername}
                        onClose={closeUser}
                    />
                ) : null}
            </div>
        </main>
    );
}
