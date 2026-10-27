import { Download, Mail, Plus, UserPlus } from "lucide-react";
import dynamic from "next/dynamic";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { usePendingInvitationCount } from "@/features/admin/hooks/usePendingInvitationCount";
import { useUserCounts } from "@/features/admin/hooks/useUserCounts";
import type { UsersFilters } from "@/features/admin/users/search-params";

const CreateUserDialog = dynamic(
    () => import("./CreateUserDialog").then((dialogModule) => dialogModule.CreateUserDialog),
    { ssr: false },
);
const ExportUsersDialog = dynamic(
    () => import("./ExportUsersDialog").then((dialogModule) => dialogModule.ExportUsersDialog),
    { ssr: false },
);
const InviteSettersDialog = dynamic(
    () =>
        import("../invitations/InviteSettersDialog").then(
            (dialogModule) => dialogModule.InviteSettersDialog,
        ),
    { ssr: false },
);

const InvitationsDialog = dynamic(
    () =>
        import("../invitations/InvitationsDialog").then(
            (dialogModule) => dialogModule.InvitationsDialog,
        ),
    { ssr: false },
);

type OpenDialog = "create" | "export" | "invite" | "invitations" | null;

interface UsersHeaderProps {
    filters: UsersFilters;
    filteredCount: number | null;
    selectedUsernames: string[];
}

export function UsersHeader({ filters, filteredCount, selectedUsernames }: UsersHeaderProps) {
    const { data: counts } = useUserCounts();
    const pendingInvitationCount = usePendingInvitationCount();
    const [openDialog, setOpenDialog] = useState<OpenDialog>(null);
    const closeDialog = () => setOpenDialog(null);

    return (
        <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
                <h1 className="text-[26px] font-extrabold">Người dùng</h1>
                <p className="mt-0.5 text-[13px] text-muted-foreground">
                    Quản lý tài khoản, vai trò và trạng thái
                </p>
            </div>
            <div className="flex flex-wrap gap-2">
                <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => setOpenDialog("export")}
                >
                    <Download aria-hidden="true" />
                    Xuất CSV
                </Button>
                <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => setOpenDialog("invite")}
                >
                    <UserPlus aria-hidden="true" />
                    Mời người ra đề
                </Button>
                <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => setOpenDialog("invitations")}
                >
                    <Mail aria-hidden="true" />
                    {pendingInvitationCount === null
                        ? "Lời mời"
                        : `Lời mời (${pendingInvitationCount})`}
                </Button>
                <Button type="button" size="sm" onClick={() => setOpenDialog("create")}>
                    <Plus aria-hidden="true" />
                    Tạo người dùng
                </Button>
            </div>
            {openDialog === "create" ? <CreateUserDialog onClose={closeDialog} /> : null}
            {openDialog === "invite" ? <InviteSettersDialog onClose={closeDialog} /> : null}
            {openDialog === "invitations" ? <InvitationsDialog onClose={closeDialog} /> : null}
            {openDialog === "export" ? (
                <ExportUsersDialog
                    filters={filters}
                    filteredCount={filteredCount}
                    totalCount={counts?.total ?? null}
                    selectedUsernames={selectedUsernames}
                    onClose={closeDialog}
                />
            ) : null}
        </div>
    );
}
