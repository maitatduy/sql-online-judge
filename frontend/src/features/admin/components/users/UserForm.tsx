import { zodResolver } from "@hookform/resolvers/zod";
import { useId, useState } from "react";
import { useForm, useWatch, type UseFormSetError } from "react-hook-form";

import type { AvatarChange } from "@/features/admin/users/avatar-file";
import { fieldErrorOf } from "@/features/admin/users/form-errors";
import { ROLE_LABELS, ROLES } from "@/features/admin/users/role-labels";
import { userFormSchema, type UserFormValues } from "@/features/admin/users/schemas";
import { ApiRequestError } from "@/lib/api-client";

import { AvatarField } from "./AvatarField";
import { DialogActions } from "./DialogActions";
import { NativeSelect } from "./NativeSelect";
import { TextInputField } from "./TextInputField";

const FORM_FIELDS = ["username", "email", "displayName"] as const;
const CONFLICT_MESSAGES: Record<string, [field: "username" | "email", message: string]> = {
    USERNAME_TAKEN: ["username", "Tên người dùng đã có người dùng"],
    EMAIL_TAKEN: ["email", "Email đã được dùng cho tài khoản khác"],
};

export interface UserFormSubmission {
    values: UserFormValues;
    avatar: AvatarChange;
}

interface UserFormProps {
    mode: "create" | "edit";
    defaultValues: UserFormValues;
    currentAvatarUrl: string | null;
    isSelf: boolean;
    submitLabel: string;
    onSubmit: (submission: UserFormSubmission) => Promise<void>;
    onFailure: (error: Error, values: UserFormValues) => void;
}

function showFieldErrors(error: unknown, setError: UseFormSetError<UserFormValues>): boolean {
    const conflict = error instanceof ApiRequestError ? CONFLICT_MESSAGES[error.code] : undefined;
    if (conflict) {
        setError(conflict[0], { message: conflict[1] });
        return true;
    }
    const fieldErrors = FORM_FIELDS.flatMap((field) => {
        const message = fieldErrorOf(error, field);
        return message ? [[field, message] as const] : [];
    });
    fieldErrors.forEach(([field, message]) => setError(field, { message }));
    return fieldErrors.length > 0;
}

export function UserForm(props: UserFormProps) {
    const { mode, defaultValues, currentAvatarUrl, isSelf, submitLabel } = props;
    const roleId = useId();
    const [avatar, setAvatar] = useState<AvatarChange>({ kind: "keep" });
    const [isSubmitting, setIsSubmitting] = useState(false);
    const {
        control,
        register,
        handleSubmit,
        setError,
        formState: { errors, isDirty },
    } = useForm<UserFormValues>({ resolver: zodResolver(userFormSchema), defaultValues });
    const [username, email] = useWatch({ control, name: ["username", "email"] });
    const isEditing = mode === "edit";
    const hasChanges = isEditing
        ? isDirty || avatar.kind !== "keep"
        : username.trim() !== "" && email.trim() !== "";

    const submitForm = (values: UserFormValues) => {
        setIsSubmitting(true);
        return props
            .onSubmit({ values, avatar })
            .catch((error: Error) => {
                if (!showFieldErrors(error, setError)) {
                    props.onFailure(error, values);
                }
            })
            .finally(() => setIsSubmitting(false));
    };

    return (
        <form noValidate onSubmit={handleSubmit(submitForm)} className="flex flex-col gap-4">
            <AvatarField currentUrl={currentAvatarUrl} change={avatar} onChange={setAvatar} />
            <div className="grid gap-4 sm:grid-cols-2">
                <TextInputField
                    label="Tên người dùng"
                    placeholder="Ví dụ: lan.pham"
                    isReadOnly={isEditing}
                    error={errors.username?.message}
                    registration={register("username")}
                />
                <TextInputField
                    label="Email"
                    type="email"
                    placeholder="Ví dụ: lan.pham@example.com"
                    isReadOnly={isEditing}
                    error={errors.email?.message}
                    registration={register("email")}
                />
                <TextInputField
                    label="Tên hiển thị"
                    placeholder="Ví dụ: Phạm Ngọc Lan"
                    error={errors.displayName?.message}
                    registration={register("displayName")}
                />
                <div>
                    <label htmlFor={roleId} className="mb-1.5 block text-[13px] font-semibold">
                        Vai trò
                    </label>
                    <NativeSelect
                        id={roleId}
                        disabled={isSelf}
                        className="w-full"
                        {...register("role")}
                    >
                        {ROLES.map((role) => (
                            <option key={role} value={role}>
                                {ROLE_LABELS[role]}
                            </option>
                        ))}
                    </NativeSelect>
                    {isSelf ? (
                        <p className="mt-1.5 text-xs text-muted-foreground">
                            Bạn không thể tự đổi vai trò của chính mình.
                        </p>
                    ) : null}
                </div>
            </div>
            <DialogActions
                submitLabel={submitLabel}
                isSubmitDisabled={!hasChanges || isSubmitting}
            />
        </form>
    );
}
