import { zodResolver } from "@hookform/resolvers/zod";
import { useId } from "react";
import { useForm, useWatch } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { useInviteUsers } from "@/features/admin/hooks/useInviteUsers";
import { parseEmailList } from "@/features/admin/users/email-list";
import { fieldErrorOf } from "@/features/admin/users/form-errors";
import { ROLE_LABELS } from "@/features/admin/users/role-labels";
import {
    INVITE_ROLES,
    inviteUsersSchema,
    type InviteUsersValues,
} from "@/features/admin/users/schemas";
import { formatNumber } from "@/lib/format";

import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";
import { NativeSelect } from "./NativeSelect";
import { TextAreaField } from "./TextAreaField";
import { TextInputField } from "./TextInputField";

const LABEL_CLASS = "mb-1.5 block text-[13px] font-semibold";

interface InviteDialogProps {
    onClose: () => void;
}

export function InviteDialog({ onClose }: InviteDialogProps) {
    const roleId = useId();
    const showToast = useToast();
    const inviteUsers = useInviteUsers();
    const {
        control,
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<InviteUsersValues>({
        resolver: zodResolver(inviteUsersSchema),
        defaultValues: { emails: "", role: "SETTER", message: "" },
    });
    const emails = useWatch({ control, name: "emails" });

    const submitInvite = ({ emails: emailText, role, message }: InviteUsersValues) =>
        inviteUsers
            .mutateAsync({ emails: parseEmailList(emailText), role, message: message || undefined })
            .then(({ invitedCount }) => {
                showToast(`Đã gửi ${formatNumber(invitedCount)} lời mời.`);
                onClose();
            })
            .catch((error: Error) => {
                const emailsError = fieldErrorOf(error, "emails");
                if (emailsError) {
                    setError("emails", { message: emailsError });
                    return;
                }
                showToast(`Chưa gửi được lời mời. ${error.message}`, "error");
            });

    return (
        <ActionDialog
            title="Mời người ra đề"
            description="Người được mời nhận email kèm liên kết kích hoạt quyền, có hiệu lực 7 ngày."
            onClose={onClose}
        >
            <form noValidate onSubmit={handleSubmit(submitInvite)} className="flex flex-col gap-4">
                <TextAreaField
                    label="Email"
                    placeholder="Mỗi dòng một email"
                    error={errors.emails?.message}
                    registration={register("emails")}
                />
                <div>
                    <label htmlFor={roleId} className={LABEL_CLASS}>
                        Vai trò
                    </label>
                    <NativeSelect id={roleId} className="w-full" {...register("role")}>
                        {INVITE_ROLES.map((role) => (
                            <option key={role} value={role}>
                                {ROLE_LABELS[role]}
                            </option>
                        ))}
                    </NativeSelect>
                </div>
                <TextInputField
                    label="Lời nhắn"
                    placeholder="Ví dụ: Chào mừng CLB tham gia ra đề cho SQL Judge"
                    error={errors.message?.message}
                    registration={register("message")}
                />
                <DialogActions
                    submitLabel="Gửi lời mời"
                    isSubmitDisabled={emails.trim() === "" || inviteUsers.isPending}
                />
            </form>
        </ActionDialog>
    );
}
