"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { CircleAlert, CircleCheck, LockKeyhole } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { useApiFormErrors } from "../hooks/useApiFormErrors";
import { useResetPassword } from "../hooks/useResetPassword";
import { resetPasswordSchema, type ResetPasswordFormValues } from "../schemas";

import { AuthStatusCard } from "./AuthStatusCard";
import { FormAlert } from "./FormAlert";
import { PasswordField } from "./PasswordField";
import { PasswordStrength } from "./PasswordStrength";

interface ResetPasswordFlowProps {
    token?: string;
}

type Step = "form" | "done" | "expired";

const INVALID_LINK_CODE = "TOKEN_INVALID_OR_EXPIRED";
const API_FIELDS = ["newPassword"] as const;

export function ResetPasswordFlow({ token }: ResetPasswordFlowProps) {
    const [step, setStep] = useState<Step>(token ? "form" : "expired");

    useEffect(() => {
        if (token) {
            window.history.replaceState(null, "", ROUTES.resetPassword);
        }
    }, [token]);

    if (step === "done") {
        return (
            <AuthStatusCard
                icon={CircleCheck}
                tone="brand"
                title="Đã đổi mật khẩu"
                description="Bạn đã có thể đăng nhập bằng mật khẩu mới. Các phiên đăng nhập trên thiết bị khác đã được đăng xuất."
            >
                <Button asChild size="lg" className="h-12.5 text-[15px]">
                    <Link href={ROUTES.login}>Đăng nhập ngay</Link>
                </Button>
            </AuthStatusCard>
        );
    }

    if (step === "expired" || !token) {
        return (
            <AuthStatusCard
                icon={CircleAlert}
                tone="destructive"
                title="Liên kết đã hết hạn"
                description="Liên kết đặt lại chỉ dùng được một lần và trong 30 phút. Hãy yêu cầu liên kết mới."
            >
                <Button asChild size="lg" className="h-12.5 text-[15px]">
                    <Link href={ROUTES.forgotPassword}>Gửi liên kết mới</Link>
                </Button>
            </AuthStatusCard>
        );
    }

    return (
        <NewPasswordForm
            token={token}
            onDone={() => setStep("done")}
            onExpired={() => setStep("expired")}
        />
    );
}

interface NewPasswordFormProps {
    token: string;
    onDone: () => void;
    onExpired: () => void;
}

function NewPasswordForm({ token, onDone, onExpired }: NewPasswordFormProps) {
    const resetPassword = useResetPassword();
    const {
        register,
        handleSubmit,
        setError,
        control,
        formState: { errors },
    } = useForm<ResetPasswordFormValues>({
        resolver: zodResolver(resetPasswordSchema),
        defaultValues: { newPassword: "", confirmPassword: "" },
    });
    const newPassword = useWatch({ control, name: "newPassword" });
    const { formError, clearFormError, handleApiError } = useApiFormErrors(setError, API_FIELDS);

    const handleSave = (values: ResetPasswordFormValues) => {
        clearFormError();
        resetPassword.mutate(
            { token, newPassword: values.newPassword },
            {
                onSuccess: onDone,
                onError: (error) => {
                    if (error instanceof ApiRequestError && error.code === INVALID_LINK_CODE) {
                        onExpired();
                        return;
                    }
                    handleApiError(error);
                },
            },
        );
    };

    return (
        <AuthStatusCard
            icon={LockKeyhole}
            tone="brand"
            title="Đặt mật khẩu mới"
            description="Sau khi đổi, mọi thiết bị đang đăng nhập sẽ bị đăng xuất."
        >
            <form noValidate onSubmit={handleSubmit(handleSave)} className="flex flex-col gap-4">
                {formError && <FormAlert message={formError} />}
                <div className="flex flex-col gap-2.5">
                    <PasswordField
                        id="reset-new-password"
                        label="Mật khẩu mới"
                        autoComplete="new-password"
                        error={errors.newPassword?.message}
                        {...register("newPassword")}
                    />
                    <PasswordStrength password={newPassword} />
                </div>
                <PasswordField
                    id="reset-confirm-password"
                    label="Nhập lại mật khẩu mới"
                    autoComplete="new-password"
                    error={errors.confirmPassword?.message}
                    {...register("confirmPassword")}
                />
                <Button
                    type="submit"
                    size="lg"
                    disabled={resetPassword.isPending}
                    className="h-12.5 text-[15px]"
                >
                    {resetPassword.isPending ? "Đang lưu…" : "Lưu mật khẩu mới"}
                </Button>
            </form>
        </AuthStatusCard>
    );
}
