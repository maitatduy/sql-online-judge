import type { Metadata } from "next";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";

export const metadata: Metadata = {
    title: "Tổng quan",
};

export default function AdminOverviewPage() {
    return (
        <main id={MAIN_CONTENT_ID} className="px-4 pt-7 pb-12 md:px-8">
            <h1 className="text-[26px] font-extrabold">Tổng quan</h1>
        </main>
    );
}
