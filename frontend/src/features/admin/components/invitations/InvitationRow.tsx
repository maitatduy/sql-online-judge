import { RotateCw, X } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { RoleBadge } from "@/features/admin/components/users/RoleBadge";
import {
    useResendInvitation,
    useRevokeInvitation,
} from "@/features/admin/hooks/useInvitationActions";
import { INVITATION_STATUS_LABELS } from "@/features/admin/invitations/status-labels";
import type { RoleInvitationSummary } from "@/features/admin/invitations/types";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";

interface InvitationRowProps {
    invitation: RoleInvitationSummary;
}

export function InvitationRow({ invitation }: InvitationRowProps) {
    const showToast = useToast();
    const revoke = useRevokeInvitation();
    const resend = useResendInvitation();
    const { email, status } = invitation;
    const isPending = status === "PENDING";
    const canResend = isPending || status === "EXPIRED";
    const isBusy = revoke.isPending || resend.isPending;

    function handleRevoke() {
        revoke.mutate(invitation.id, {
            onSuccess: () => showToast(`Đã huỷ lời mời gửi tới ${email}.`),
            onError: (failure) => showToast(`Chưa huỷ được lời mời. ${failure.message}`, "error"),
        });
    }

    function handleResend() {
        resend.mutate(invitation.id, {
            onSuccess: () => showToast(`Đã gửi lại lời mời tới ${email}.`),
            onError: (failure) =>
                showToast(`Chưa gửi lại được lời mời. ${failure.message}`, "error"),
        });
    }

    return (
        <tr className="border-t">
            <td className="max-w-[220px] truncate px-3 py-2.5 font-medium">{email}</td>
            <td className="px-3 py-2.5">
                <RoleBadge role={invitation.role} />
            </td>
            <td className="px-3 py-2.5">
                <span
                    className={cn(
                        "inline-flex h-5.5 items-center rounded-full bg-surface-2 px-2 text-xs font-semibold whitespace-nowrap",
                        isPending ? "text-foreground" : "text-muted-foreground",
                    )}
                >
                    {INVITATION_STATUS_LABELS[status]}
                </span>
            </td>
            <td className="px-3 py-2.5 text-muted-foreground">
                {invitation.inviterUsername ?? "-"}
            </td>
            <td className="px-3 py-2.5 whitespace-nowrap text-muted-foreground">
                {formatDate(invitation.invitedAt)}
            </td>
            <td className="px-3 py-2.5">
                <div className="flex justify-end gap-1">
                    {canResend ? (
                        <Button
                            type="button"
                            size="icon"
                            variant="ghost"
                            className="size-8"
                            aria-label={`Gửi lại lời mời ${email}`}
                            title="Gửi lại"
                            disabled={isBusy}
                            onClick={handleResend}
                        >
                            <RotateCw aria-hidden="true" />
                        </Button>
                    ) : null}
                    {isPending ? (
                        <Button
                            type="button"
                            size="icon"
                            variant="ghost"
                            className="size-8 text-destructive"
                            aria-label={`Huỷ lời mời ${email}`}
                            title="Huỷ lời mời"
                            disabled={isBusy}
                            onClick={handleRevoke}
                        >
                            <X aria-hidden="true" />
                        </Button>
                    ) : null}
                </div>
            </td>
        </tr>
    );
}
