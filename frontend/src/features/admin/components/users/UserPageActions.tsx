import { Pencil, RotateCcw, Trash2 } from "lucide-react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import type { AdminUserDetail } from "@/features/admin/users/types";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

const EditUserDialog = dynamic(
    () => import("./EditUserDialog").then((dialogModule) => dialogModule.EditUserDialog),
    { ssr: false },
);
const BanUserDialog = dynamic(
    () => import("./BanUserDialog").then((dialogModule) => dialogModule.BanUserDialog),
    { ssr: false },
);
const UnbanUserDialog = dynamic(
    () => import("./UnbanUserDialog").then((dialogModule) => dialogModule.UnbanUserDialog),
    { ssr: false },
);
const DeleteUserDialog = dynamic(
    () => import("./DeleteUserDialog").then((dialogModule) => dialogModule.DeleteUserDialog),
    { ssr: false },
);
const RestoreUserDialog = dynamic(
    () => import("./RestoreUserDialog").then((dialogModule) => dialogModule.RestoreUserDialog),
    { ssr: false },
);

type OpenDialog = "edit" | "ban" | "ban-permanent" | "unban" | "delete" | "restore" | null;

const DANGER_CLASS = "border-destructive/40 text-destructive hover:text-destructive";

interface UserPageActionsProps {
    user: Pick<AdminUserDetail, "username" | "status" | "deletedAt">;
}

export function UserPageActions({ user }: UserPageActionsProps) {
    const { data: currentUser } = useCurrentUser();
    const [openDialog, setOpenDialog] = useState<OpenDialog>(null);
    const closeDialog = () => setOpenDialog(null);
    const isDeleted = user.deletedAt !== null;
    const isBanned = user.status === "BANNED";
    const isOtherUser = currentUser !== undefined && currentUser.username !== user.username;

    return (
        <div className="flex flex-wrap gap-2">
            {isDeleted ? (
                <Button type="button" size="sm" onClick={() => setOpenDialog("restore")}>
                    <RotateCcw aria-hidden="true" />
                    Khôi phục
                </Button>
            ) : (
                <>
                    <Button asChild size="sm" variant="outline">
                        <Link href={ROUTES.profile(user.username)}>Hồ sơ công khai</Link>
                    </Button>
                    <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={() => setOpenDialog("edit")}
                    >
                        <Pencil aria-hidden="true" />
                        Sửa
                    </Button>
                    {isBanned ? (
                        <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            onClick={() => setOpenDialog("unban")}
                        >
                            Mở khoá tài khoản
                        </Button>
                    ) : null}
                    {isOtherUser && !isBanned ? (
                        <>
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={() => setOpenDialog("ban")}
                            >
                                Khoá tạm thời
                            </Button>
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={() => setOpenDialog("ban-permanent")}
                                className={DANGER_CLASS}
                            >
                                Khoá vĩnh viễn
                            </Button>
                        </>
                    ) : null}
                    {isOtherUser ? (
                        <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            onClick={() => setOpenDialog("delete")}
                            className={DANGER_CLASS}
                        >
                            <Trash2 aria-hidden="true" />
                            Xoá tài khoản
                        </Button>
                    ) : null}
                </>
            )}
            {openDialog === "edit" ? (
                <EditUserDialog username={user.username} onClose={closeDialog} />
            ) : null}
            {openDialog === "ban" ? (
                <BanUserDialog username={user.username} onClose={closeDialog} />
            ) : null}
            {openDialog === "ban-permanent" ? (
                <BanUserDialog
                    username={user.username}
                    initialDuration="PERMANENT"
                    onClose={closeDialog}
                />
            ) : null}
            {openDialog === "unban" ? (
                <UnbanUserDialog username={user.username} onClose={closeDialog} />
            ) : null}
            {openDialog === "delete" ? (
                <DeleteUserDialog
                    username={user.username}
                    onClose={closeDialog}
                    onDeleted={closeDialog}
                />
            ) : null}
            {openDialog === "restore" ? (
                <RestoreUserDialog username={user.username} onClose={closeDialog} />
            ) : null}
        </div>
    );
}
