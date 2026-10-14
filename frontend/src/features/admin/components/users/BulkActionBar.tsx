import dynamic from "next/dynamic";
import Link from "next/link";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

const BulkBanDialog = dynamic(
    () => import("./BulkBanDialog").then((dialogModule) => dialogModule.BulkBanDialog),
    { ssr: false },
);
const BulkRoleDialog = dynamic(
    () => import("./BulkRoleDialog").then((dialogModule) => dialogModule.BulkRoleDialog),
    { ssr: false },
);

type OpenDialog = "ban" | "role" | null;

interface BulkActionBarProps {
    selectedUsernames: string[];
    onClear: () => void;
}

export function BulkActionBar({ selectedUsernames, onClear }: BulkActionBarProps) {
    const { data: currentUser } = useCurrentUser();
    const [openDialog, setOpenDialog] = useState<OpenDialog>(null);
    const selfUsername = currentUser?.username ?? null;
    const skippedSelf =
        selfUsername && selectedUsernames.includes(selfUsername) ? selfUsername : null;
    const targetUsernames = selectedUsernames.filter((username) => username !== selfUsername);
    const closeDialog = () => setOpenDialog(null);
    const finishAction = () => {
        setOpenDialog(null);
        onClear();
    };

    return (
        <div
            role="region"
            aria-label="Thao tác hàng loạt"
            className="flex flex-wrap items-center gap-2.5 border-b border-brand/30 bg-brand-soft px-3.5 py-2 text-[13px]"
        >
            <strong className="text-brand">{`Đã chọn ${formatNumber(selectedUsernames.length)}`}</strong>
            <Button type="button" size="sm" variant="ghost" onClick={onClear}>
                Bỏ chọn
            </Button>
            <span className="flex-1" />
            <Button type="button" size="sm" variant="outline" onClick={() => setOpenDialog("role")}>
                Đổi vai trò
            </Button>
            <Button asChild size="sm" variant="outline">
                <Link href={ROUTES.adminMessages}>Gửi thông báo</Link>
            </Button>
            <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => setOpenDialog("ban")}
                className="border-destructive/40 text-destructive hover:text-destructive"
            >
                Khoá tài khoản
            </Button>
            {openDialog === "ban" ? (
                <BulkBanDialog
                    usernames={targetUsernames}
                    skippedSelf={skippedSelf}
                    onClose={closeDialog}
                    onDone={finishAction}
                />
            ) : null}
            {openDialog === "role" ? (
                <BulkRoleDialog
                    usernames={targetUsernames}
                    skippedSelf={skippedSelf}
                    onClose={closeDialog}
                    onDone={finishAction}
                />
            ) : null}
        </div>
    );
}
