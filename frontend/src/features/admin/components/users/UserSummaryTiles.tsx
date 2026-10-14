import { Skeleton } from "@/components/shared/Skeleton";
import { useUserCounts } from "@/features/admin/hooks/useUserCounts";
import type { UsersFilterChange } from "@/features/admin/hooks/useUsersFilters";
import type { AccountState } from "@/features/admin/users/account-status";
import type { UsersFilters } from "@/features/admin/users/search-params";
import type { UserCountsResponse } from "@/features/admin/users/types";
import type { Role } from "@/lib/current-user";
import { formatNumber } from "@/lib/format";

import { SummaryTile } from "./SummaryTile";

const UNKNOWN_COUNT_LABEL = "-";

interface TileGroup {
    label: string;
    countKey: keyof UserCountsResponse;
    role: Role | null;
    state: AccountState | null;
    valueClassName?: string;
}

const TILE_GROUPS: TileGroup[] = [
    { label: "Tổng tài khoản", countKey: "total", role: null, state: null },
    {
        label: "Chưa xác minh email",
        countKey: "unverified",
        role: null,
        state: "UNVERIFIED",
        valueClassName: "text-account-unverified",
    },
    { label: "Người ra đề", countKey: "setters", role: "SETTER", state: null },
    { label: "Kiểm duyệt", countKey: "moderators", role: "MODERATOR", state: null },
    {
        label: "Đang bị khoá",
        countKey: "banned",
        role: null,
        state: "BANNED",
        valueClassName: "text-account-banned",
    },
];

interface UserSummaryTilesProps {
    filters: UsersFilters;
    onChange: (change: UsersFilterChange) => void;
}

export function UserSummaryTiles({ filters, onChange }: UserSummaryTilesProps) {
    const { data: counts, isPending } = useUserCounts();

    return (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
            {TILE_GROUPS.map(({ label, countKey, role, state, valueClassName }) => (
                <SummaryTile
                    key={countKey}
                    label={label}
                    value={
                        isPending ? (
                            <Skeleton className="my-1.5 h-5 w-12" />
                        ) : counts ? (
                            formatNumber(counts[countKey])
                        ) : (
                            UNKNOWN_COUNT_LABEL
                        )
                    }
                    isSelected={
                        filters.q === "" && filters.role === role && filters.state === state
                    }
                    onSelect={() => onChange({ q: "", role, state })}
                    valueClassName={valueClassName}
                />
            ))}
        </div>
    );
}
