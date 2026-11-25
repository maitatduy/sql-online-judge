"use client";

import { Avatar } from "@/components/shared/Avatar";
import { formatNumber } from "@/lib/format";

import { useContributors } from "../hooks/useCommunity";
import { ProblemDiscussionsCard } from "./ProblemDiscussionsCard";

const RULES = [
    "Không đăng đáp án bài đang trong contest.",
    "Lời giải ghi rõ dialect.",
    "Tôn trọng người mới.",
];

export function CommunityAside() {
    const { data: contributors } = useContributors();

    return (
        <aside className="flex flex-col gap-4">
            <ProblemDiscussionsCard />
            {contributors && contributors.length > 0 ? (
                <section className="flex flex-col gap-3 rounded-lg border bg-card p-5">
                    <h2 className="text-base font-bold">Đóng góp nhiều tuần này</h2>
                    <ol className="flex flex-col gap-3">
                        {contributors.map((contributor) => (
                            <li key={contributor.userId} className="flex items-center gap-2.5">
                                <Avatar
                                    name={contributor.username ?? "?"}
                                    className="size-7 text-[11px]"
                                />
                                <span className="flex-1 font-semibold">
                                    {contributor.username ?? "Người dùng"}
                                </span>
                                <span className="text-sm text-muted-foreground">
                                    {`+${formatNumber(contributor.points)}`}
                                </span>
                            </li>
                        ))}
                    </ol>
                </section>
            ) : null}
            <section className="flex flex-col gap-2 rounded-lg border bg-card p-5">
                <h2 className="text-base font-bold">Quy tắc</h2>
                <ol className="list-decimal pl-5 text-[15px] leading-relaxed">
                    {RULES.map((rule) => (
                        <li key={rule}>{rule}</li>
                    ))}
                </ol>
            </section>
        </aside>
    );
}
