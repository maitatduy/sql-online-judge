import type { Metadata } from "next";

import { AppProviders } from "@/app/providers";
import { SkipLink } from "@/components/shared/SkipLink";
import { ToastProvider } from "@/components/shared/ToastProvider";
import { AdminShell } from "@/features/admin/components/AdminShell";

export const metadata: Metadata = {
    title: {
        default: "Admin Console",
        template: "%s | Admin Console",
    },
    robots: { index: false, follow: false },
};

export default function AdminLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <AppProviders>
            <div className="min-h-screen bg-surface">
                <SkipLink />
                <ToastProvider>
                    <AdminShell>{children}</AdminShell>
                </ToastProvider>
            </div>
        </AppProviders>
    );
}
