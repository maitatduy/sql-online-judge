import { AppProviders } from "@/app/providers";
import { SkipLink } from "@/components/shared/SkipLink";

export default function AuthLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <AppProviders>
            <div className="flex min-h-screen flex-col bg-background bg-[radial-gradient(var(--surface-2)_1px,transparent_1px)] [background-size:22px_22px]">
                <SkipLink />
                {children}
            </div>
        </AppProviders>
    );
}
