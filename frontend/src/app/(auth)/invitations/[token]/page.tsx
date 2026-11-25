import { ChevronLeft } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";

import { AuthStandaloneShell } from "@/features/auth/components/AuthStandaloneShell";
import { InvitationPanel } from "@/features/invitations/components/InvitationPanel";
import { ROUTES } from "@/lib/routes";

export const metadata: Metadata = {
    title: "Lời mời tham gia đội ngũ",
    robots: { index: false },
};

interface InvitationPageProps {
    params: Promise<{ token: string }>;
}

export default async function InvitationPage({ params }: InvitationPageProps) {
    const { token } = await params;
    return (
        <AuthStandaloneShell
            headerAside={
                <Link
                    href={ROUTES.home}
                    className="inline-flex min-h-11 items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
                >
                    <ChevronLeft aria-hidden="true" className="size-4" />
                    Về trang chủ
                </Link>
            }
        >
            <InvitationPanel token={token} />
        </AuthStandaloneShell>
    );
}
