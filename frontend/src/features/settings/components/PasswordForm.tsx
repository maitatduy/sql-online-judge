"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import { fieldErrorsOf } from "../field-errors";
import { useChangePassword } from "../hooks/useAccountSettings";
import { changePasswordSchema, PASSWORD_FIELDS, type ChangePasswordValues } from "../schemas";

import { FIELD_CONTROL_CLASS, SettingsField } from "./SettingsField";

const EMPTY_VALUES: ChangePasswordValues = {
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
};

interface PasswordFormProps {
    hasPassword: boolean;
}

export function PasswordForm({ hasPassword }: PasswordFormProps) {
    const showToast = useToast();
    const change = useChangePassword();
    const {
        register,
        handleSubmit,
        setError,
        reset,
        formState: { errors },
    } = useForm<ChangePasswordValues>({
        resolver: zodResolver(changePasswordSchema),
        defaultValues: EMPTY_VALUES,
    });
    const title = hasPassword ? "Đổi mật khẩu" : "Đặt mật khẩu";

    const handleChange = async (values: ChangePasswordValues) => {
        try {
            await change.mutateAsync({
                currentPassword: hasPassword ? values.currentPassword : undefined,
                newPassword: values.newPassword,
            });
            reset(EMPTY_VALUES);
            showToast(hasPassword ? "Đã đổi mật khẩu" : "Đã đặt mật khẩu");
        } catch (error) {
            const fieldErrors = fieldErrorsOf(error, PASSWORD_FIELDS);
            fieldErrors.forEach(({ field, message }) => setError(field, { message }));
            if (fieldErrors.length === 0) {
                showToast((error as Error).message, "error");
            }
        }
    };

    return (
        <section aria-labelledby="password-settings" className="rounded-lg border bg-card p-6">
            <h2 id="password-settings" className="mb-1 text-base font-bold">
                {title}
            </h2>
            {hasPassword ? null : (
                <p className="mb-4 text-sm text-muted-foreground">
                    Bạn đang đăng nhập bằng GitHub hoặc Google. Đặt mật khẩu để đăng nhập bằng
                    email.
                </p>
            )}
            <form noValidate onSubmit={handleSubmit(handleChange)} className="mt-3 grid gap-4">
                {hasPassword ? (
                    <SettingsField
                        label="Mật khẩu hiện tại"
                        error={errors.currentPassword?.message}
                    >
                        {(control) => (
                            <input
                                {...control}
                                type="password"
                                autoComplete="current-password"
                                className={FIELD_CONTROL_CLASS}
                                {...register("currentPassword")}
                            />
                        )}
                    </SettingsField>
                ) : null}
                <div className="grid gap-4 sm:grid-cols-2">
                    <SettingsField label="Mật khẩu mới" error={errors.newPassword?.message}>
                        {(control) => (
                            <input
                                {...control}
                                type="password"
                                autoComplete="new-password"
                                className={FIELD_CONTROL_CLASS}
                                {...register("newPassword")}
                            />
                        )}
                    </SettingsField>
                    <SettingsField
                        label="Nhập lại mật khẩu mới"
                        error={errors.confirmPassword?.message}
                    >
                        {(control) => (
                            <input
                                {...control}
                                type="password"
                                autoComplete="new-password"
                                className={FIELD_CONTROL_CLASS}
                                {...register("confirmPassword")}
                            />
                        )}
                    </SettingsField>
                </div>
                <div>
                    <Button type="submit" disabled={change.isPending}>
                        {title}
                    </Button>
                </div>
            </form>
        </section>
    );
}
