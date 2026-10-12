import type { Metadata } from "next";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { AiSection } from "@/features/marketing/components/AiSection";
import { CtaBanner } from "@/features/marketing/components/CtaBanner";
import { HeroSection } from "@/features/marketing/components/HeroSection";
import { PillarsSection } from "@/features/marketing/components/PillarsSection";
import { RoadmapSection } from "@/features/marketing/components/RoadmapSection";
import { StatsStrip } from "@/features/marketing/components/StatsStrip";
import { TopRating } from "@/features/marketing/components/TopRating";
import { UpcomingContests } from "@/features/marketing/components/UpcomingContests";
import {
    ROADMAP_STAGES,
    SAMPLE_STATS,
    SAMPLE_TOP_RATED_USERS,
    SAMPLE_UPCOMING_CONTESTS,
} from "@/features/marketing/sample-data";
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
            <StatsStrip stats={SAMPLE_STATS} />
            <PillarsSection />
            <AiSection />
            <div className="mx-auto flex max-w-7xl flex-wrap gap-10 px-6 py-16 md:py-24">
                <UpcomingContests contests={SAMPLE_UPCOMING_CONTESTS} />
                <TopRating users={SAMPLE_TOP_RATED_USERS} />
            </div>
            <RoadmapSection stages={ROADMAP_STAGES} />
            <CtaBanner />
        </main>
    );
}
