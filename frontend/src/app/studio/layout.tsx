import type { Metadata } from "next";

import { AppProviders } from "@/app/providers";
import { SkipLink } from "@/components/shared/SkipLink";
import { ToastProvider } from "@/components/shared/ToastProvider";
import { StudioShell } from "@/features/studio/components/StudioShell";

export const metadata: Metadata = {
    title: {
        default: "Khu vực người ra đề",
        template: "%s | Khu vực người ra đề",
    },
    robots: { index: false, follow: false },
};

export default function StudioLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <AppProviders>
            <div className="min-h-screen bg-background">
                <SkipLink />
                <ToastProvider>
                    <StudioShell>{children}</StudioShell>
                </ToastProvider>
            </div>
        </AppProviders>
    );
}
