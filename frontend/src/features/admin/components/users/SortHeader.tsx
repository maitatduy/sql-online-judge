import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";

import { nextUsersSort, type UsersSort, type UsersSortField } from "@/features/admin/users/sort";
import { cn } from "@/lib/utils";

interface SortHeaderProps {
    label: string;
    field: UsersSortField;
    sort: UsersSort | null;
    onSortChange: (sort: UsersSort) => void;
    className?: string;
}

export function SortHeader({ label, field, sort, onSortChange, className }: SortHeaderProps) {
    const direction = sort?.field === field ? sort.direction : null;
    const Icon = direction === null ? ArrowUpDown : direction === "desc" ? ArrowDown : ArrowUp;
    const ariaSort =
        direction === null ? "none" : direction === "desc" ? "descending" : "ascending";

    return (
        <th scope="col" aria-sort={ariaSort} className={className}>
            <button
                type="button"
                onClick={() => onSortChange(nextUsersSort(sort, field))}
                className={cn(
                    "inline-flex items-center gap-1 font-semibold max-md:min-h-11",
                    direction === null ? null : "text-foreground",
                )}
            >
                {label}
                <Icon aria-hidden="true" className="size-3" />
            </button>
        </th>
    );
}
