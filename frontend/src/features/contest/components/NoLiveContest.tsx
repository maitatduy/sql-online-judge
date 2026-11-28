import { ArrowRight } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import type { ContestSummary } from "../types";
import { Countdown } from "./Countdown";

interface NoLiveContestProps {
    next?: ContestSummary;
}

export function NoLiveContest({ next }: NoLiveContestProps) {
    return (
        <section
            aria-label="Chưa có contest đang diễn ra"
            className="grid items-center gap-8 rounded-xl border border-contest-hero-line bg-contest-hero p-6 text-contest-hero-text md:p-10 lg:grid-cols-[minmax(0,1fr)_auto]"
        >
            <div className="flex min-w-0 items-start gap-5">
                <div className="flex min-w-0 flex-col gap-2">
                    <h2 className="text-[28px] leading-tight font-extrabold tracking-tight">
                        Chưa có contest nào đang diễn ra
                    </h2>
                    <p className="text-lg text-contest-hero-muted">
                        {next
                            ? `Contest tiếp theo: ${next.title}. Đăng ký sớm để không bỏ lỡ.`
                            : "Contest mới sẽ sớm được công bố."}
                    </p>
                    <div className="mt-2 flex flex-wrap gap-3">
                        {next ? (
                            <Button asChild size="lg">
                                <Link href={ROUTES.contest(next.slug)}>
                                    Xem contest tiếp theo
                                    <ArrowRight aria-hidden="true" />
                                </Link>
                            </Button>
                        ) : null}
                        <Button asChild size="lg" variant="outline">
                            <Link href={ROUTES.problems}>Luyện tập trong lúc chờ</Link>
                        </Button>
                    </div>
                </div>
            </div>
            {next ? <Countdown label="Bắt đầu sau" target={next.startAt} /> : null}
        </section>
    );
}
