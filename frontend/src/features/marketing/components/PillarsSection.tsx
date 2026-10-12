import { ArrowBigUp, BookOpen, Code, MessageSquare, Trophy } from "lucide-react";

import { VerdictBadge } from "@/components/shared/VerdictBadge";

import { PillarCard } from "./PillarCard";
import { SectionHeading } from "./SectionHeading";

export function PillarsSection() {
    return (
        <section className="mx-auto max-w-7xl px-6 py-16 md:py-24">
            <SectionHeading
                eyebrow="Bốn trụ cột"
                title="Từ câu SELECT đầu tiên đến bảng xếp hạng contest"
                className="mb-10 max-w-[720px]"
            />
            <div className="grid grid-cols-[repeat(auto-fit,minmax(260px,1fr))] gap-5">
                <PillarCard
                    icon={BookOpen}
                    iconClassName="bg-brand-soft text-brand"
                    title="Học"
                    description="Lộ trình có thứ tự, mỗi bài giảng có ô SQL chạy được ngay giữa đoạn văn."
                >
                    <div className="flex justify-between">
                        <span>JOIN · bài 14/24</span>
                        <span className="text-muted-foreground">58%</span>
                    </div>
                    <div
                        aria-hidden="true"
                        className="mt-2 h-1.5 overflow-hidden rounded-full bg-surface-2"
                    >
                        <div className="h-full w-[58%] rounded-full bg-brand" />
                    </div>
                </PillarCard>
                <PillarCard
                    icon={Code}
                    iconClassName="bg-verdict-ac/10 text-verdict-ac"
                    title="Luyện"
                    description="Kho bài theo chủ đề và độ khó, chấm với testcase ẩn, xem diff khi sai."
                >
                    <div className="flex flex-wrap gap-1.5">
                        <VerdictBadge verdict="AC" />
                        <VerdictBadge verdict="WA" />
                        <VerdictBadge verdict="OM" />
                        <VerdictBadge verdict="TLE" />
                    </div>
                </PillarCard>
                <PillarCard
                    icon={Trophy}
                    iconClassName="bg-verdict-om/10 text-verdict-om"
                    title="Thi"
                    description="Contest có rating, bảng xếp hạng realtime, đóng băng 30 phút cuối."
                >
                    <ol className="flex flex-col gap-1.5">
                        <li className="flex justify-between">
                            <span>1. quang.le</span>
                            <span className="font-mono">400 · 38′</span>
                        </li>
                        <li className="flex justify-between">
                            <span>2. trang.hoang</span>
                            <span className="font-mono">400 · 51′</span>
                        </li>
                    </ol>
                </PillarCard>
                <PillarCard
                    icon={MessageSquare}
                    iconClassName="bg-surface-2 text-foreground"
                    title="Cộng đồng"
                    description="Hỏi đáp, chia sẻ lời giải, kinh nghiệm phỏng vấn - code trong bài viết chạy được."
                >
                    <strong>Dùng DENSE_RANK cho bài #184</strong>
                    <p className="mt-0.5 flex items-center gap-1 text-muted-foreground">
                        <ArrowBigUp aria-hidden="true" className="size-3.5" />
                        <span className="sr-only">Lượt bình chọn:</span>
                        128 · 24 bình luận
                    </p>
                </PillarCard>
            </div>
        </section>
    );
}
