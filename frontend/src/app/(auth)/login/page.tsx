import type { Metadata } from "next";

import { AuthShell } from "@/features/auth/components/AuthShell";
import { LoginForm } from "@/features/auth/components/LoginForm";
import { getSafeNextPath } from "@/lib/safe-redirect";

export const metadata: Metadata = {
    title: "Đăng nhập",
    description: "Đăng nhập để tiếp tục lộ trình và bài đang làm dở.",
};

interface LoginPageProps {
    searchParams: Promise<{ next?: string | string[]; error?: string | string[] }>;
}

export default async function LoginPage({ searchParams }: LoginPageProps) {
    const { next, error } = await searchParams;
    const nextPath = getSafeNextPath(next);
    const oauthErrorCode = typeof error === "string" ? error : undefined;

    return (
        <AuthShell mode="login" nextPath={nextPath}>
            <LoginForm nextPath={nextPath} oauthErrorCode={oauthErrorCode} />
        </AuthShell>
    );
}
