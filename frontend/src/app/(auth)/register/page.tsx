import type { Metadata } from "next";

import { AuthShell } from "@/features/auth/components/AuthShell";
import { RegisterForm } from "@/features/auth/components/RegisterForm";
import { getSafeNextPath } from "@/lib/safe-redirect";

export const metadata: Metadata = {
    title: "Đăng ký",
    description: "Tạo tài khoản miễn phí để học SQL với máy chấm tự động.",
};

interface RegisterPageProps {
    searchParams: Promise<{ next?: string | string[] }>;
}

export default async function RegisterPage({ searchParams }: RegisterPageProps) {
    const { next } = await searchParams;
    const nextPath = getSafeNextPath(next);

    return (
        <AuthShell mode="register" nextPath={nextPath}>
            <RegisterForm nextPath={nextPath} />
        </AuthShell>
    );
}
