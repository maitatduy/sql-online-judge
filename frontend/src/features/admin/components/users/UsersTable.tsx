import { useState } from "react";

import type { UserSelection } from "@/features/admin/hooks/useUserSelection";
import { pageSelectionOf } from "@/features/admin/users/selection";
import type { UsersSort } from "@/features/admin/users/sort";
import type { AdminUserSummary } from "@/features/admin/users/types";

import { SortHeader } from "./SortHeader";
import { CHECKBOX_CLASS, HEADER_CELL_CLASS } from "./table-cells";
import { UserRow } from "./UserRow";

interface UsersTableProps {
    users: AdminUserSummary[];
    sort: UsersSort | null;
    onSortChange: (sort: UsersSort) => void;
    openUsername: string | null;
    onOpenUser: (username: string) => void;
    selection: UserSelection;
}

export function UsersTable({
    users,
    sort,
    onSortChange,
    openUsername,
    onOpenUser,
    selection,
}: UsersTableProps) {
    const [mountedAt] = useState(() => new Date());
    const pageUsernames = users.map((user) => user.username);
    const pageSelection = pageSelectionOf(selection.selected, pageUsernames);

    return (
        <div role="region" aria-label="Bảng người dùng" tabIndex={0} className="overflow-x-auto">
            <table className="w-full min-w-[760px] border-collapse text-[13px]">
                <thead>
                    <tr>
                        <td className={`${HEADER_CELL_CLASS} w-9`}>
                            <input
                                type="checkbox"
                                aria-label="Chọn tất cả trên trang"
                                checked={pageSelection === "all"}
                                ref={(checkbox) => {
                                    if (checkbox) {
                                        checkbox.indeterminate = pageSelection === "some";
                                    }
                                }}
                                onChange={() => selection.togglePage(pageUsernames)}
                                className={CHECKBOX_CLASS}
                            />
                        </td>
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Người dùng
                        </th>
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Vai trò
                        </th>
                        <SortHeader
                            label="Rating"
                            field="rating"
                            sort={sort}
                            onSortChange={onSortChange}
                            className={`${HEADER_CELL_CLASS} text-right`}
                        />
                        <SortHeader
                            label="Bài AC"
                            field="acceptedCount"
                            sort={sort}
                            onSortChange={onSortChange}
                            className={`${HEADER_CELL_CLASS} text-right`}
                        />
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Tham gia
                        </th>
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Hoạt động
                        </th>
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Trạng thái
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {users.map((user) => (
                        <UserRow
                            key={user.id}
                            user={user}
                            now={mountedAt}
                            isOpen={user.username === openUsername}
                            isSelected={selection.selected.has(user.username)}
                            onOpen={onOpenUser}
                            onToggleSelected={selection.toggleUser}
                        />
                    ))}
                </tbody>
            </table>
        </div>
    );
}
