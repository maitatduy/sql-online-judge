import { Check } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

const REASONS = [
    "Hỗ trợ PostgreSQL, MySQL và SQLite",
    "Máy chấm trả kết quả trong vài giây",
    "Trợ lý AI gợi ý khi bạn bí",
];

export function CtaBanner() {
    return (
        <section className="mx-auto max-w-7xl px-6 py-16 md:py-24">
            <div className="grid items-center gap-10 rounded-xl bg-brand p-8 text-white md:p-16 lg:grid-cols-[1fr_auto]">
                <div>
                    <p className="text-xs font-semibold tracking-[0.06em] uppercase">
                        Bắt đầu ngay
                    </p>
                    <h2 className="mt-3 text-3xl font-extrabold tracking-[-0.02em] md:text-5xl md:leading-[1.1]">
                        Câu truy vấn đầu tiên chỉ mất 30 giây.
                    </h2>
                    <p className="mt-4 max-w-xl text-[17px] leading-[1.6] text-white/95">
                        Không cần cài đặt cơ sở dữ liệu. Miễn phí cho người học. Mở trình duyệt,
                        chọn một bài và viết truy vấn ngay.
                    </p>
                    <div className="mt-8 flex flex-wrap gap-3">
                        <Button
                            asChild
                            size="lg"
                            variant="outline"
                            className="border-white bg-white text-brand hover:bg-white/90 hover:text-brand focus-visible:outline-white"
                        >
                            <Link href={ROUTES.register}>Bắt đầu miễn phí</Link>
                        </Button>
                        <Button
                            asChild
                            size="lg"
                            variant="outline"
                            className="border-white/60 bg-transparent text-white hover:bg-white/10 hover:text-white focus-visible:outline-white"
                        >
                            <Link href={ROUTES.problems}>Xem bài tập</Link>
                        </Button>
                    </div>
                </div>
                <ul className="flex flex-col gap-3">
                    {REASONS.map((reason) => (
                        <li
                            key={reason}
                            className="flex items-center gap-3 rounded-lg border border-white/30 px-4 py-3.5 font-semibold"
                        >
                            <Check aria-hidden="true" className="size-4 shrink-0" />
                            {reason}
                        </li>
                    ))}
                </ul>
            </div>
        </section>
    );
}
