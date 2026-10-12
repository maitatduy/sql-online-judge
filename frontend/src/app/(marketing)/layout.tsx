import { SkipLink } from "@/components/shared/SkipLink";
import { MarketingFooter } from "@/features/marketing/components/MarketingFooter";
import { MarketingHeader } from "@/features/marketing/components/MarketingHeader";

export default function MarketingLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <>
            <SkipLink />
            <MarketingHeader />
            {children}
            <MarketingFooter />
        </>
    );
}
