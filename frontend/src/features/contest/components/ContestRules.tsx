import { LockKeyhole } from "lucide-react";

import { formatTime } from "@/lib/format";

import type { ContestDetail } from "../types";

interface ContestRulesProps {
    contest: ContestDetail;
}

function rulesOf(contest: ContestDetail): string[] {
    return [
        "Xếp hạng theo số bài giải được, hoà thì so tổng phạt.",
        `Mỗi lần nộp sai trước khi AC phạt ${contest.penaltyMinutes} phút, lỗi cú pháp không tính.`,
        ...(contest.freezeMinutes > 0
            ? [`Bảng xếp hạng đóng băng ${contest.freezeMinutes} phút cuối.`]
            : []),
        ...(contest.isRated ? ["Trợ lý AI bị khoá trong giờ thi."] : []),
    ];
}

export function ContestRules({ contest }: ContestRulesProps) {
    return (
        <>
            <section className="rounded-lg border bg-card p-5">
                <h2 className="mb-3 text-base font-bold">Luật thi</h2>
                <ol className="flex list-decimal flex-col gap-2 pl-5">
                    {rulesOf(contest).map((rule) => (
                        <li key={rule}>{rule}</li>
                    ))}
                </ol>
            </section>
            <section className="flex items-center gap-4 rounded-lg border border-dashed bg-card p-5">
                <LockKeyhole aria-hidden="true" className="size-7 text-muted-foreground" />
                <p className="font-bold">{`Đề bài mở lúc ${formatTime(contest.startAt)}`}</p>
            </section>
        </>
    );
}
