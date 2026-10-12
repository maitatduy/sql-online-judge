import type { Metadata } from "next";

import { AuthShell } from "@/features/auth/components/AuthShell";
import { LoginForm } from "@/features/auth/components/LoginForm";
import { getSafeNextPath } from "@/lib/safe-redirect";

export const metadata: Metadata = {
    title: "Đăng nhập",
    description: "Đăng nhập để tiếp tục lộ trình và bài đang làm dở.",
};

interface LoginPageProps {
    searchParams: Promise<{ next?: string | string[] }>;
}

export default async function LoginPage({ searchParams }: LoginPageProps) {
    const { next } = await searchParams;
    const nextPath = getSafeNextPath(next);

    return (
        <AuthShell mode="login" nextPath={nextPath}>
            <LoginForm nextPath={nextPath} />
        </AuthShell>
    );
}
