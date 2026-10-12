import { Check } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import { CtaEditorPreview } from "./CtaEditorPreview";

const REASONS = [
    "Hỗ trợ PostgreSQL, MySQL và SQLite",
    "Máy chấm trả kết quả trong vài giây",
    "Trợ lý AI gợi ý khi bạn bí",
];

export function CtaBanner() {
    return (
        <section className="mx-auto grid max-w-7xl items-start gap-10 px-6 py-16 md:py-24 lg:grid-cols-[3fr_2fr] lg:gap-14">
            <div className="min-w-0 lg:order-2">
                <h2 className="text-3xl font-extrabold tracking-[-0.02em] md:text-[40px] md:leading-[1.15]">
                    Câu truy vấn đầu tiên chỉ mất 30 giây.
                </h2>
                <p className="mt-4 text-[17px] leading-[1.6] text-muted-foreground">
                    Không cần cài đặt cơ sở dữ liệu. Miễn phí cho người học.
                </p>
                <ul className="mt-6 flex flex-col gap-3">
                    {REASONS.map((reason) => (
                        <li key={reason} className="flex items-center gap-2.5 font-semibold">
                            <Check aria-hidden="true" className="size-4 shrink-0 text-brand" />
                            {reason}
                        </li>
                    ))}
                </ul>
                <div className="mt-8 flex flex-wrap gap-3">
                    <Button asChild size="lg">
                        <Link href={ROUTES.register}>Bắt đầu miễn phí</Link>
                    </Button>
                    <Button asChild size="lg" variant="outline">
                        <Link href={ROUTES.problems}>Xem bài tập</Link>
                    </Button>
                </div>
            </div>
            <CtaEditorPreview />
        </section>
    );
}
