import { Search } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { UsersFilterChange } from "@/features/admin/hooks/useUsersFilters";
import {
    ACCOUNT_STATE_LABELS,
    ACCOUNT_STATES,
    type AccountState,
} from "@/features/admin/users/account-status";
import { ROLE_LABELS, ROLES } from "@/features/admin/users/role-labels";
import { hasActiveFilter, type UsersFilters } from "@/features/admin/users/search-params";
import type { Role } from "@/lib/current-user";

const SEARCH_DEBOUNCE_MS = 300;

interface UserFiltersProps {
    filters: UsersFilters;
    onChange: (change: UsersFilterChange) => void;
    onClear: () => void;
}

export function UserFilters({ filters, onChange, onClear }: UserFiltersProps) {
    const [searchText, setSearchText] = useState(filters.q);
    const [appliedQuery, setAppliedQuery] = useState(filters.q);
    const pendingSearch = useRef<ReturnType<typeof setTimeout>>(undefined);

    if (filters.q !== appliedQuery) {
        setAppliedQuery(filters.q);
        if (filters.q !== searchText.trim()) {
            setSearchText(filters.q);
        }
    }

    useEffect(() => () => clearTimeout(pendingSearch.current), []);

    function handleSearchChange(nextText: string) {
        setSearchText(nextText);
        clearTimeout(pendingSearch.current);
        pendingSearch.current = setTimeout(
            () => onChange({ q: nextText.trim() }),
            SEARCH_DEBOUNCE_MS,
        );
    }

    function handleClear() {
        clearTimeout(pendingSearch.current);
        setSearchText("");
        onClear();
    }

    return (
        <div className="flex flex-wrap items-center gap-2 border-b px-3.5 py-3">
            <div className="relative min-w-55 flex-1">
                <Search
                    aria-hidden="true"
                    className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                    type="search"
                    aria-label="Tìm người dùng"
                    placeholder="Tên người dùng hoặc email…"
                    value={searchText}
                    onChange={(event) => handleSearchChange(event.target.value)}
                    className="pl-8.5 shadow-none"
                />
            </div>
            <NativeSelect
                aria-label="Lọc theo vai trò"
                value={filters.role ?? ""}
                onChange={(event) =>
                    onChange({ role: (event.target.value || null) as Role | null })
                }
            >
                <option value="">Mọi vai trò</option>
                {ROLES.map((role) => (
                    <option key={role} value={role}>
                        {ROLE_LABELS[role]}
                    </option>
                ))}
            </NativeSelect>
            <NativeSelect
                aria-label="Lọc theo trạng thái"
                value={filters.state ?? ""}
                onChange={(event) =>
                    onChange({ state: (event.target.value || null) as AccountState | null })
                }
            >
                <option value="">Mọi trạng thái</option>
                {ACCOUNT_STATES.map((state) => (
                    <option key={state} value={state}>
                        {ACCOUNT_STATE_LABELS[state]}
                    </option>
                ))}
            </NativeSelect>
            {hasActiveFilter(filters) ? (
                <Button type="button" variant="ghost" onClick={handleClear}>
                    Xoá lọc
                </Button>
            ) : null}
        </div>
    );
}
