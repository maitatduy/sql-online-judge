import type { Metadata } from "next";

import { AuthStandaloneShell } from "@/features/auth/components/AuthStandaloneShell";
import { BackToLoginLink } from "@/features/auth/components/BackToLoginLink";
import { OAuthCallback } from "@/features/auth/components/OAuthCallback";

export const metadata: Metadata = {
    title: "Đang đăng nhập",
    robots: { index: false },
};

export default function OAuthCallbackPage() {
    return (
        <AuthStandaloneShell headerAside={<BackToLoginLink />}>
            <OAuthCallback />
        </AuthStandaloneShell>
    );
}
