import type { Metadata } from "next";

import { AuthStandaloneShell } from "@/features/auth/components/AuthStandaloneShell";
import { VerifyEmailFlow } from "@/features/auth/components/VerifyEmailFlow";

export const metadata: Metadata = {
    title: "Xác thực email",
    robots: { index: false },
};

interface VerifyEmailPageProps {
    searchParams: Promise<{ token?: string | string[] }>;
}

export default async function VerifyEmailPage({ searchParams }: VerifyEmailPageProps) {
    const { token } = await searchParams;
    const linkToken = typeof token === "string" && token ? token : undefined;

    return (
        <AuthStandaloneShell
            headerAside={
                <span className="text-[13px] text-muted-foreground">Bước 2/3 · Xác thực email</span>
            }
        >
            <VerifyEmailFlow token={linkToken} />
        </AuthStandaloneShell>
    );
}
