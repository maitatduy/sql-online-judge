"use client";

import { CircleAlert, CircleCheck, Mail } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState, useSyncExternalStore } from "react";

import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { RESEND_COOLDOWN_SECONDS } from "../countdown";
import { getFormErrorMessage } from "../form-errors";
import { useConfirmEmail } from "../hooks/useConfirmEmail";
import { readPendingVerificationEmail } from "../pending-verification";
import type { AuthResponse } from "../types";

import { ResendCodeButton } from "./ResendCodeButton";
import { VerificationCodeForm } from "./VerificationCodeForm";
import { AuthStatusCard } from "./AuthStatusCard";

interface VerifyEmailFlowProps {
    token?: string;
}

type Step = "sent" | "verified" | "invalid";

const UNPROCESSABLE_STATUS = 422;
const LINK_CLASS =
    "inline-flex min-h-11 items-center justify-center text-[13px] font-semibold text-brand underline-offset-4 hover:underline md:min-h-0";

const subscribe = () => () => undefined;
const readOnServer = () => null;

export function VerifyEmailFlow({ token }: VerifyEmailFlowProps) {
    const email = useSyncExternalStore(subscribe, readPendingVerificationEmail, readOnServer);
    const [step, setStep] = useState<Step>("sent");
    const [verifiedUsername, setVerifiedUsername] = useState<string | null>(null);
    const confirmLink = useConfirmEmail();
    const hasRequestedLink = useRef(false);

    const handleVerified = ({ user }: AuthResponse) => {
        setVerifiedUsername(user.username);
        setStep("verified");
    };

    const confirmToken = (linkToken: string) => {
        confirmLink.mutate(
            { token: linkToken },
            {
                onSuccess: handleVerified,
                onError: (error) => {
                    if (error instanceof ApiRequestError && error.status === UNPROCESSABLE_STATUS) {
                        setStep("invalid");
                    }
                },
            },
        );
    };

    useEffect(() => {
        if (!token || hasRequestedLink.current) {
            return;
        }
        hasRequestedLink.current = true;
        confirmToken(token);
    });

    if (step === "verified") {
        return (
            <AuthStatusCard
                icon={CircleCheck}
                tone="brand"
                title="Email đã được xác thực"
                description={
                    <>
                        Tài khoản <strong className="text-foreground">{verifiedUsername}</strong> đã
                        sẵn sàng. Bắt đầu học SQL cùng máy chấm tự động.
                    </>
                }
            >
                <Button asChild size="lg" className="h-12.5 text-[15px]">
                    <Link href={ROUTES.home}>Tiếp tục</Link>
                </Button>
            </AuthStatusCard>
        );
    }

    if (step === "invalid") {
        return (
            <AuthStatusCard
                icon={CircleAlert}
                tone="destructive"
                title="Mã không hợp lệ hoặc đã hết hạn"
                description="Mã xác thực có hiệu lực 15 phút và chỉ dùng được một lần. Hãy gửi mã mới."
            >
                {email && <ResendCodeButton email={email} cooldownSeconds={0} />}
                <Link href={ROUTES.login} className={LINK_CLASS}>
                    Quay lại đăng nhập
                </Link>
            </AuthStatusCard>
        );
    }

    if (token && confirmLink.isError) {
        return (
            <AuthStatusCard
                icon={CircleAlert}
                tone="destructive"
                title="Chưa xác thực được email"
                description={getFormErrorMessage(confirmLink.error)}
            >
                <Button
                    size="lg"
                    className="h-12.5 text-[15px]"
                    onClick={() => confirmToken(token)}
                >
                    Thử lại
                </Button>
            </AuthStatusCard>
        );
    }

    if (token) {
        return (
            <AuthStatusCard
                icon={Mail}
                tone="brand"
                title="Đang xác thực email"
                description={<span role="status">Đang kiểm tra liên kết xác thực…</span>}
            />
        );
    }

    return (
        <AuthStatusCard
            icon={Mail}
            tone="brand"
            title="Xác thực email của bạn"
            description={
                <>
                    Chúng tôi vừa gửi mã 6 số tới{" "}
                    {email ? (
                        <strong className="text-foreground">{email}</strong>
                    ) : (
                        "email bạn đã đăng ký"
                    )}
                    . {email ? "Nhập mã bên dưới hoặc bấm" : "Bấm"} liên kết trong email. Mã có hiệu
                    lực 15 phút.
                </>
            }
        >
            {email && (
                <>
                    <VerificationCodeForm email={email} onVerified={handleVerified} />
                    <ResendCodeButton email={email} cooldownSeconds={RESEND_COOLDOWN_SECONDS} />
                </>
            )}
            <p className="text-center text-[13px] text-muted-foreground">
                Nhập sai email?{" "}
                <Link href={ROUTES.register} className={LINK_CLASS}>
                    Đổi email
                </Link>
            </p>
        </AuthStatusCard>
    );
}
