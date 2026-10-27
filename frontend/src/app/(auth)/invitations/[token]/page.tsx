import type { Metadata } from "next";

import { InvitationPanel } from "@/features/invitations/components/InvitationPanel";

export const metadata: Metadata = {
    title: "Lời mời tham gia đội ngũ",
    robots: { index: false },
};

interface InvitationPageProps {
    params: Promise<{ token: string }>;
}

export default async function InvitationPage({ params }: InvitationPageProps) {
    const { token } = await params;
    return <InvitationPanel token={token} />;
}
