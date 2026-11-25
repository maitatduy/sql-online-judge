import type { Metadata } from "next";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { CtaBanner } from "@/features/marketing/components/CtaBanner";
import { HeroSection } from "@/features/marketing/components/HeroSection";
import { RoadmapChart } from "@/features/marketing/components/RoadmapChart";
import { StatsStrip } from "@/features/marketing/components/StatsStrip";
import { TopRating } from "@/features/marketing/components/TopRating";
import { UpcomingContests } from "@/features/marketing/components/UpcomingContests";
import { SITE_NAME } from "@/lib/site";

const PAGE_TITLE = `${SITE_NAME} - Học SQL bằng cách viết SQL`;
const PAGE_DESCRIPTION =
    "Viết truy vấn SQL ngay trên trình duyệt, máy chấm trả kết quả trong vài giây, thi đấu hằng tuần với cộng đồng.";

export const metadata: Metadata = {
    title: { absolute: PAGE_TITLE },
    description: PAGE_DESCRIPTION,
    openGraph: {
        title: PAGE_TITLE,
        description: PAGE_DESCRIPTION,
        type: "website",
        locale: "vi_VN",
        siteName: SITE_NAME,
    },
};

export default function LandingPage() {
    return (
        <main id={MAIN_CONTENT_ID}>
            <HeroSection />
            <StatsStrip />
            <RoadmapChart />
            <div className="mx-auto flex max-w-7xl flex-wrap gap-10 px-6 py-16 md:py-24">
                <UpcomingContests />
                <TopRating />
            </div>
            <CtaBanner />
        </main>
    );
}
