"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";

import { formatCountdown } from "../countdown";
import { getFormErrorMessage } from "../form-errors";
import { useCountdown } from "../hooks/useCountdown";
import { useResendVerification } from "../hooks/useResendVerification";

interface ResendCodeButtonProps {
    email: string;
    cooldownSeconds: number;
}

export function ResendCodeButton({ email, cooldownSeconds }: ResendCodeButtonProps) {
    const { secondsLeft, restart } = useCountdown(cooldownSeconds);
    const resend = useResendVerification();
    const [hasResent, setHasResent] = useState(false);
    const isWaiting = secondsLeft > 0;

    const handleResend = () => {
        resend.mutate(
            { email },
            {
                onSuccess: () => {
                    setHasResent(true);
                    restart();
                },
            },
        );
    };

    return (
        <div className="flex flex-col gap-2">
            <Button
                type="button"
                variant="outline"
                size="lg"
                disabled={isWaiting || resend.isPending}
                onClick={handleResend}
                className="h-11.5"
            >
                {isWaiting ? (
                    <>
                        Gửi lại mã sau{" "}
                        <span className="font-mono">{formatCountdown(secondsLeft)}</span>
                    </>
                ) : (
                    "Gửi lại mã"
                )}
            </Button>
            <p role="status" className="text-center text-[13px] text-muted-foreground">
                {hasResent && "Đã gửi mã mới tới email của bạn"}
            </p>
            {resend.isError && (
                <p className="text-center text-[13px] text-destructive">
                    {getFormErrorMessage(resend.error)}
                </p>
            )}
        </div>
    );
}
