import type { UserSelection } from "@/features/admin/hooks/useUserSelection";
import { pageSelectionOf } from "@/features/admin/users/selection";
import type { AdminUserSummary } from "@/features/admin/users/types";

import { CHECKBOX_CLASS, CHECKBOX_TARGET_CLASS, HEADER_CELL_CLASS } from "./table-cells";
import { UserRow } from "./UserRow";
import type { UserRowHandlers } from "./UserRowActions";

interface UsersTableProps {
    users: AdminUserSummary[];
    selection: UserSelection;
    selfUsername: string | null;
    rowHandlers: UserRowHandlers;
}

export function UsersTable({ users, selection, selfUsername, rowHandlers }: UsersTableProps) {
    const pageUsernames = users.map((user) => user.username);
    const pageSelection = pageSelectionOf(selection.selected, pageUsernames);

    return (
        <div role="region" aria-label="Bảng người dùng" tabIndex={0} className="overflow-x-auto">
            <table className="w-full min-w-[560px] border-collapse text-[13px]">
                <thead>
                    <tr>
                        <td className={`${HEADER_CELL_CLASS} w-9`}>
                            <label className={CHECKBOX_TARGET_CLASS}>
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
                            </label>
                        </td>
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Người dùng
                        </th>
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Vai trò
                        </th>
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            Trạng thái
                        </th>
                        <th scope="col" className={`${HEADER_CELL_CLASS} text-right`}>
                            Hành động
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {users.map((user) => (
                        <UserRow
                            key={user.id}
                            user={user}
                            isSelected={selection.selected.has(user.username)}
                            onToggleSelected={selection.toggleUser}
                            canDelete={user.username !== selfUsername && user.role !== "ADMIN"}
                            handlers={rowHandlers}
                        />
                    ))}
                </tbody>
            </table>
        </div>
    );
}
