import { useId, useState } from "react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { useToast } from "@/components/shared/ToastProvider";
import { ActionDialog } from "@/features/admin/components/users/ActionDialog";
import { DialogActions } from "@/features/admin/components/users/DialogActions";
import { useInviteRoles } from "@/features/admin/hooks/useInviteRoles";
import { usePendingInvitationEmails } from "@/features/admin/hooks/usePendingInvitationEmails";
import type { InvitableRole, InviteRolesResponse } from "@/features/admin/invitations/types";
import { ROLE_LABELS } from "@/features/admin/users/role-labels";

import { InviteeList } from "./InviteeList";
import { InviteeSearch, type Invitee } from "./InviteeSearch";
import { InviteResult } from "./InviteResult";

const MAX_INVITEES = 20;
const MAX_MESSAGE_LENGTH = 500;
const INVITABLE_ROLES: InvitableRole[] = ["SETTER", "MODERATOR"];

interface InviteSettersDialogProps {
    onClose: () => void;
}

export function InviteSettersDialog({ onClose }: InviteSettersDialogProps) {
    const roleId = useId();
    const messageId = useId();
    const showToast = useToast();
    const inviteRoles = useInviteRoles();
    const pendingEmails = usePendingInvitationEmails();
    const [role, setRole] = useState<InvitableRole>("SETTER");
    const [invitees, setInvitees] = useState<Invitee[]>([]);
    const [message, setMessage] = useState("");
    const [result, setResult] = useState<InviteRolesResponse | null>(null);
    const selectedEmails = new Set(invitees.map((invitee) => invitee.email));

    function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        inviteRoles.mutate(
            {
                emails: invitees.map((invitee) => invitee.email),
                role,
                message: message.trim() || undefined,
            },
            {
                onSuccess: setResult,
                onError: (failure) =>
                    showToast(`Chưa gửi được lời mời. ${failure.message}`, "error"),
            },
        );
    }

    return (
        <ActionDialog
            title="Mời người ra đề"
            description="Người được mời nhận email kèm liên kết kích hoạt quyền, có hiệu lực 7 ngày."
            onClose={onClose}
            className="max-w-[560px]"
        >
            {result ? (
                <InviteResult result={result} onDone={onClose} />
            ) : (
                <form noValidate onSubmit={handleSubmit} className="flex flex-col gap-4">
                    <div>
                        <label htmlFor={roleId} className="mb-1.5 block text-[13px] font-semibold">
                            Vai trò
                        </label>
                        <NativeSelect
                            id={roleId}
                            className="w-full"
                            value={role}
                            onChange={(event) => setRole(event.target.value as InvitableRole)}
                        >
                            {INVITABLE_ROLES.map((invitableRole) => (
                                <option key={invitableRole} value={invitableRole}>
                                    {ROLE_LABELS[invitableRole]}
                                </option>
                            ))}
                        </NativeSelect>
                    </div>
                    <InviteeSearch
                        role={role}
                        pendingEmails={pendingEmails}
                        selectedEmails={selectedEmails}
                        isFull={invitees.length >= MAX_INVITEES}
                        onAdd={(invitee) => setInvitees((current) => [...current, invitee])}
                    />
                    <div className="flex flex-col gap-1.5">
                        <p className="flex justify-between text-[13px] font-semibold">
                            Người sẽ được mời
                            <span className="font-mono font-normal text-muted-foreground">
                                {invitees.length}/{MAX_INVITEES}
                            </span>
                        </p>
                        <InviteeList
                            invitees={invitees}
                            onRemove={(email) =>
                                setInvitees((current) =>
                                    current.filter((invitee) => invitee.email !== email),
                                )
                            }
                        />
                    </div>
                    <div>
                        <label
                            htmlFor={messageId}
                            className="mb-1.5 block text-[13px] font-semibold"
                        >
                            Lời nhắn
                        </label>
                        <textarea
                            id={messageId}
                            rows={2}
                            placeholder="Không bắt buộc"
                            maxLength={MAX_MESSAGE_LENGTH}
                            value={message}
                            onChange={(event) => setMessage(event.target.value)}
                            className="w-full rounded-md border border-input bg-transparent px-2.5 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                        />
                    </div>
                    <DialogActions
                        submitLabel={
                            invitees.length > 0 ? `Gửi ${invitees.length} lời mời` : "Gửi lời mời"
                        }
                        isSubmitDisabled={invitees.length === 0 || inviteRoles.isPending}
                    />
                </form>
            )}
        </ActionDialog>
    );
}
