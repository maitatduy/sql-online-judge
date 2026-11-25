import { AppProviders } from "@/app/providers";
import { SkipLink } from "@/components/shared/SkipLink";
import { TopBar } from "@/components/shared/TopBar";
import { MarketingFooter } from "@/features/marketing/components/MarketingFooter";

export default function MarketingLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <AppProviders>
            <SkipLink />
            <TopBar />
            {children}
            <MarketingFooter />
        </AppProviders>
    );
}
