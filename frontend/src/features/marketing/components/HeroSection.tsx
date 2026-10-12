import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import { HeroEditorPreview } from "./HeroEditorPreview";

export function HeroSection() {
    return (
        <section className="mx-auto flex max-w-7xl flex-wrap items-center gap-10 px-6 pt-12 pb-16 md:gap-14 md:pt-18 md:pb-22">
            <div className="min-w-0 flex-[1_1_460px]">
                <h1 className="text-[40px] leading-[1.05] font-extrabold tracking-[-0.035em] sm:text-5xl md:text-[64px]">
                    Học SQL bằng cách <span className="text-brand">viết SQL</span>.
                </h1>
                <p className="mt-6 max-w-[520px] text-[19px] leading-[1.6] text-muted-foreground">
                    Viết truy vấn ngay trên trình duyệt, máy chấm trả kết quả trong vài giây, thi
                    đấu hằng tuần với cộng đồng.
                </p>
                <div className="mt-8 flex flex-wrap gap-3">
                    <Button asChild size="lg">
                        <Link href={ROUTES.register}>Bắt đầu miễn phí</Link>
                    </Button>
                    <Button asChild size="lg" variant="outline">
                        <Link href={ROUTES.problems}>Xem bài tập</Link>
                    </Button>
                </div>
            </div>
            <HeroEditorPreview />
        </section>
    );
}
