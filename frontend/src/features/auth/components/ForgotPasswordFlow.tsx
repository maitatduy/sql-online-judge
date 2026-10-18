"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { KeyRound, Mail } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import { formatCountdown, RESEND_COOLDOWN_SECONDS } from "../countdown";
import { useApiFormErrors } from "../hooks/useApiFormErrors";
import { useCountdown } from "../hooks/useCountdown";
import { useRequestPasswordReset } from "../hooks/useRequestPasswordReset";
import { forgotPasswordSchema, type ForgotPasswordFormValues } from "../schemas";

import { AuthStatusCard } from "./AuthStatusCard";
import { FormAlert } from "./FormAlert";
import { TextField } from "./TextField";

const API_FIELDS = ["email"] as const;
const LINK_CLASS =
    "inline-flex min-h-11 items-center font-semibold text-brand underline-offset-4 hover:underline md:min-h-0";

export function ForgotPasswordFlow() {
    const [sentTo, setSentTo] = useState<string | null>(null);
    const requestReset = useRequestPasswordReset();
    const {
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<ForgotPasswordFormValues>({
        resolver: zodResolver(forgotPasswordSchema),
        defaultValues: { email: "" },
    });
    const { formError, clearFormError, handleApiError } = useApiFormErrors(setError, API_FIELDS);

    const handleRequest = ({ email }: ForgotPasswordFormValues) => {
        clearFormError();
        requestReset.mutate(
            { email },
            { onSuccess: () => setSentTo(email), onError: handleApiError },
        );
    };

    if (sentTo) {
        return <ResetLinkSent email={sentTo} onUseAnotherEmail={() => setSentTo(null)} />;
    }

    return (
        <AuthStatusCard
            icon={KeyRound}
            tone="brand"
            title="Quên mật khẩu?"
            description="Nhập email đã đăng ký. Chúng tôi sẽ gửi một liên kết để bạn đặt mật khẩu mới, có hiệu lực trong 30 phút."
        >
            <form noValidate onSubmit={handleSubmit(handleRequest)} className="flex flex-col gap-4">
                {formError && <FormAlert message={formError} />}
                <TextField
                    id="forgot-password-email"
                    label="Email"
                    icon={Mail}
                    type="email"
                    autoComplete="email"
                    error={errors.email?.message}
                    {...register("email")}
                />
                <Button
                    type="submit"
                    size="lg"
                    disabled={requestReset.isPending}
                    className="h-12.5 text-[15px]"
                >
                    {requestReset.isPending ? "Đang gửi…" : "Gửi liên kết đặt lại"}
                </Button>
            </form>
            <p className="text-center text-[13px] text-muted-foreground">
                Nhớ ra rồi?{" "}
                <Link href={ROUTES.login} className={LINK_CLASS}>
                    Đăng nhập
                </Link>
            </p>
        </AuthStatusCard>
    );
}

interface ResetLinkSentProps {
    email: string;
    onUseAnotherEmail: () => void;
}

function ResetLinkSent({ email, onUseAnotherEmail }: ResetLinkSentProps) {
    const { secondsLeft, restart } = useCountdown(RESEND_COOLDOWN_SECONDS);
    const resend = useRequestPasswordReset();
    const isWaiting = secondsLeft > 0;

    const handleResend = () => {
        resend.mutate({ email }, { onSuccess: restart });
    };

    return (
        <AuthStatusCard
            icon={Mail}
            tone="brand"
            title="Kiểm tra hộp thư"
            description={
                <>
                    Nếu <strong className="text-foreground">{email}</strong> có tài khoản trên SQL
                    Judge, bạn sẽ nhận được email kèm liên kết đặt lại mật khẩu trong vài phút.
                </>
            }
        >
            <ul className="flex flex-col gap-2 rounded-lg bg-surface-2 px-4 py-3.5 text-[13px] text-muted-foreground">
                <li>Liên kết hết hạn sau 30 phút.</li>
                <li>Không thấy? Xem thư mục Spam hoặc Quảng cáo.</li>
            </ul>
            <Button
                type="button"
                variant="outline"
                size="lg"
                onClick={onUseAnotherEmail}
                className="h-11.5"
            >
                Dùng email khác
            </Button>
            <Button
                type="button"
                variant="ghost"
                disabled={isWaiting || resend.isPending}
                onClick={handleResend}
                className="text-[13px] text-muted-foreground"
            >
                {isWaiting ? (
                    <>
                        Gửi lại email sau{" "}
                        <span className="font-mono">{formatCountdown(secondsLeft)}</span>
                    </>
                ) : (
                    "Gửi lại email"
                )}
            </Button>
        </AuthStatusCard>
    );
}
