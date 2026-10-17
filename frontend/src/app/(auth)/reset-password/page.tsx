import type { Metadata } from "next";

import { AuthStandaloneShell } from "@/features/auth/components/AuthStandaloneShell";
import { BackToLoginLink } from "@/features/auth/components/BackToLoginLink";
import { ResetPasswordFlow } from "@/features/auth/components/ResetPasswordFlow";

export const metadata: Metadata = {
    title: "Đặt lại mật khẩu",
    robots: { index: false },
};

interface ResetPasswordPageProps {
    searchParams: Promise<{ token?: string | string[] }>;
}

export default async function ResetPasswordPage({ searchParams }: ResetPasswordPageProps) {
    const { token } = await searchParams;
    const linkToken = typeof token === "string" && token ? token : undefined;

    return (
        <AuthStandaloneShell headerAside={<BackToLoginLink />}>
            <ResetPasswordFlow token={linkToken} />
        </AuthStandaloneShell>
    );
}
