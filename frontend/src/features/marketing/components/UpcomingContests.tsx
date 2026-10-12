import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ROUTES } from "@/lib/routes";

import type { UpcomingContest } from "../types";

interface UpcomingContestsProps {
    contests: UpcomingContest[];
}

export function UpcomingContests({ contests }: UpcomingContestsProps) {
    return (
        <section className="min-w-0 flex-[2_1_600px]">
            <div className="mb-5 flex items-baseline justify-between">
                <h2 className="text-[28px] font-extrabold">Contest sắp tới</h2>
                <Link
                    href={ROUTES.contests}
                    aria-label="Xem tất cả contest"
                    className="inline-flex min-h-11 items-center text-brand underline-offset-4 hover:underline md:min-h-0"
                >
                    Xem tất cả
                </Link>
            </div>
            <div className="grid grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-4">
                {contests.map((contest) => (
                    <Card key={contest.title} className="items-start gap-2.5 p-4.5">
                        <span className="inline-flex h-6 items-center rounded-full bg-brand-soft px-2.5 text-xs font-semibold text-brand">
                            {contest.startLabel}
                        </span>
                        <h3 className="text-base font-bold">{contest.title}</h3>
                        <p className="text-[13px] text-muted-foreground">
                            {contest.problemCount} bài · {contest.durationMinutes} phút ·{" "}
                            {contest.note}
                        </p>
                        <Button asChild size="sm" variant="outline">
                            <Link href={ROUTES.contests} aria-label={`Đăng ký ${contest.title}`}>
                                Đăng ký
                            </Link>
                        </Button>
                    </Card>
                ))}
            </div>
        </section>
    );
}
