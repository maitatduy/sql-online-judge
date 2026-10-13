import { AppProviders } from "@/app/providers";
import { SkipLink } from "@/components/shared/SkipLink";
import { TopBar } from "@/components/shared/TopBar";

export default function AppLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <AppProviders>
            <div className="min-h-screen bg-surface">
                <SkipLink />
                <TopBar />
                {children}
            </div>
        </AppProviders>
    );
}
