import dynamic from "next/dynamic";
import Link from "next/link";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { canModerate } from "@/features/admin/users/moderation";
import type { AdminUserDetail } from "@/features/admin/users/types";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { ResendVerificationButton } from "./ResendVerificationButton";

const BanUserDialog = dynamic(
    () => import("./BanUserDialog").then((dialogModule) => dialogModule.BanUserDialog),
    { ssr: false },
);
const PasswordResetDialog = dynamic(
    () => import("./PasswordResetDialog").then((dialogModule) => dialogModule.PasswordResetDialog),
    { ssr: false },
);
const UnbanUserDialog = dynamic(
    () => import("./UnbanUserDialog").then((dialogModule) => dialogModule.UnbanUserDialog),
    { ssr: false },
);

type OpenDialog = "ban" | "unban" | "reset" | null;

interface UserDetailActionsProps {
    user: Pick<
        AdminUserDetail,
        "username" | "email" | "role" | "status" | "isEmailVerified" | "deletedAt"
    >;
}

export function UserDetailActions({ user }: UserDetailActionsProps) {
    const { data: currentUser } = useCurrentUser();
    const [openDialog, setOpenDialog] = useState<OpenDialog>(null);
    const isDeleted = user.deletedAt !== null;
    const isBanned = !isDeleted && user.status === "BANNED";
    const canBan = !isDeleted && !isBanned && canModerate(currentUser?.username, user);
    const closeDialog = () => setOpenDialog(null);

    return (
        <div className="flex flex-col gap-2">
            <Button asChild size="sm">
                <Link href={ROUTES.adminUser(user.username)}>Mở trang chi tiết</Link>
            </Button>
            <Button asChild size="sm" variant="outline">
                <Link href={ROUTES.profile(user.username)}>Xem hồ sơ công khai</Link>
            </Button>
            {isDeleted || user.isEmailVerified ? null : (
                <ResendVerificationButton username={user.username} email={user.email} />
            )}
            {isDeleted ? null : (
                <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => setOpenDialog("reset")}
                >
                    Gửi email đặt lại mật khẩu
                </Button>
            )}
            {canBan ? (
                <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => setOpenDialog("ban")}
                    className="border-destructive/40 text-destructive hover:text-destructive"
                >
                    Khoá tài khoản
                </Button>
            ) : null}
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
            {openDialog === "ban" ? (
                <BanUserDialog username={user.username} onClose={closeDialog} />
            ) : null}
            {openDialog === "reset" ? (
                <PasswordResetDialog
                    username={user.username}
                    email={user.email}
                    onClose={closeDialog}
                />
            ) : null}
            {openDialog === "unban" ? (
                <UnbanUserDialog username={user.username} onClose={closeDialog} />
            ) : null}
        </div>
    );
}
