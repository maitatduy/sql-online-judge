import { useId, useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { useExportUsers } from "@/features/admin/hooks/useExportUsers";
import type { UsersFilters } from "@/features/admin/users/search-params";
import type { ExportScope, ExportUsersRequest } from "@/features/admin/users/types";
import { usersQueryOf } from "@/features/admin/users/users-query";
import { formatNumber } from "@/lib/format";

import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";

const OPTION_CLASS = "flex items-center gap-2 text-sm has-disabled:text-muted-foreground";
const RADIO_CLASS = "size-4 accent-brand";

interface ExportUsersDialogProps {
    filters: UsersFilters;
    filteredCount: number | null;
    totalCount: number | null;
    selectedUsernames: string[];
    onClose: () => void;
}

function exportRequestOf(
    scope: ExportScope,
    { filters, selectedUsernames }: Pick<ExportUsersDialogProps, "filters" | "selectedUsernames">,
): Omit<ExportUsersRequest, "includeEmail"> {
    if (scope === "FILTERED") {
        return { scope, filters: usersQueryOf(filters) };
    }
    return scope === "SELECTED" ? { scope, usernames: selectedUsernames } : { scope };
}

function countSuffix(count: number | null, unit: string): string {
    return count === null ? "" : `: ${formatNumber(count)} ${unit}`;
}

export function ExportUsersDialog(props: ExportUsersDialogProps) {
    const { filteredCount, totalCount, selectedUsernames, onClose } = props;
    const showToast = useToast();
    const exportUsers = useExportUsers();
    const [scope, setScope] = useState<ExportScope>("FILTERED");
    const [includeEmail, setIncludeEmail] = useState(false);
    const includeEmailHintId = useId();

    function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        exportUsers.mutate(
            { ...exportRequestOf(scope, props), includeEmail },
            {
                onSuccess: () => {
                    showToast("Đang tạo file CSV, bạn sẽ nhận thông báo khi xong.");
                    onClose();
                },
                onError: (error) => showToast(`Chưa tạo được file CSV. ${error.message}`, "error"),
            },
        );
    }

    const scopeOption = (value: ExportScope, label: string, isDisabled = false) => (
        <label className={OPTION_CLASS}>
            <input
                type="radio"
                name="export-scope"
                className={RADIO_CLASS}
                checked={scope === value}
                disabled={isDisabled}
                onChange={() => setScope(value)}
            />
            {label}
        </label>
    );

    return (
        <ActionDialog
            title="Xuất danh sách người dùng"
            description="File CSV sẽ có trong mục Thông báo khi tạo xong."
            onClose={onClose}
        >
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <fieldset className="flex flex-col gap-2">
                    <legend className="mb-1.5 text-[13px] font-semibold">Phạm vi</legend>
                    {scopeOption(
                        "FILTERED",
                        `Theo bộ lọc hiện tại${countSuffix(filteredCount, "người")}`,
                    )}
                    {scopeOption(
                        "SELECTED",
                        `Những người đã chọn: ${formatNumber(selectedUsernames.length)} người`,
                        selectedUsernames.length === 0,
                    )}
                    {scopeOption(
                        "ALL",
                        totalCount === null
                            ? "Tất cả tài khoản"
                            : `Tất cả ${formatNumber(totalCount)} tài khoản`,
                    )}
                </fieldset>
                <div>
                    <label className="flex items-center gap-2 text-[13px]">
                        <input
                            type="checkbox"
                            className="size-4 accent-brand"
                            checked={includeEmail}
                            aria-describedby={includeEmailHintId}
                            onChange={(event) => setIncludeEmail(event.target.checked)}
                        />
                        Kèm email
                    </label>
                    <p id={includeEmailHintId} className="mt-1 pl-6 text-xs text-muted-foreground">
                        Email là dữ liệu cá nhân, chỉ dùng nội bộ.
                    </p>
                </div>
                <DialogActions
                    submitLabel="Tạo file CSV"
                    isSubmitDisabled={exportUsers.isPending}
                />
            </form>
        </ActionDialog>
    );
}
