import type { Metadata } from "next";

import { AuthStandaloneShell } from "@/features/auth/components/AuthStandaloneShell";
import { BackToLoginLink } from "@/features/auth/components/BackToLoginLink";
import { ForgotPasswordFlow } from "@/features/auth/components/ForgotPasswordFlow";

export const metadata: Metadata = {
    title: "Quên mật khẩu",
    description: "Nhận liên kết đặt lại mật khẩu tài khoản SQL Judge qua email.",
};

export default function ForgotPasswordPage() {
    return (
        <AuthStandaloneShell headerAside={<BackToLoginLink />}>
            <ForgotPasswordFlow />
        </AuthStandaloneShell>
    );
}
