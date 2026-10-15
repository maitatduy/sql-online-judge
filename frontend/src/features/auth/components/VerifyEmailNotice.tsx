"use client";

import { Mail } from "lucide-react";
import Link from "next/link";
import { useSyncExternalStore } from "react";

import { ROUTES } from "@/lib/routes";

import { readPendingVerificationEmail } from "../pending-verification";

const subscribe = () => () => undefined;
const readOnServer = () => null;

export function VerifyEmailNotice() {
    const email = useSyncExternalStore(subscribe, readPendingVerificationEmail, readOnServer);

    return (
        <div className="flex flex-col gap-5 rounded-xl border bg-card p-6 shadow-2xl sm:p-8">
            <span className="inline-flex size-13 items-center justify-center rounded-xl bg-brand-soft text-brand">
                <Mail aria-hidden="true" className="size-6.5" />
            </span>
            <div>
                <h1 className="text-[26px] font-extrabold tracking-[-0.02em]">
                    Xác thực email của bạn
                </h1>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                    Chúng tôi vừa gửi mã 6 số tới{" "}
                    {email ? (
                        <strong className="text-foreground">{email}</strong>
                    ) : (
                        "email bạn đã đăng ký"
                    )}
                    . Nhập mã hoặc bấm liên kết trong email. Mã có hiệu lực 15 phút.
                </p>
            </div>
            <p className="text-center text-[13px] text-muted-foreground">
                Nhập sai email?{" "}
                <Link
                    href={ROUTES.register}
                    className="inline-flex min-h-11 items-center font-semibold text-brand underline-offset-4 hover:underline md:min-h-0"
                >
                    Đổi email
                </Link>
            </p>
        </div>
    );
}
