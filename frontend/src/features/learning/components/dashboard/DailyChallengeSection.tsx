import { ChevronRight } from "lucide-react";
import Link from "next/link";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { Skeleton } from "@/components/shared/Skeleton";
import { ROUTES } from "@/lib/routes";

import type { SuggestedProblem } from "../../types";
import { DashboardSection } from "./DashboardSection";

interface DailyChallengeSectionProps {
    problems: SuggestedProblem[] | undefined;
}

export function DailyChallengeSection({ problems }: DailyChallengeSectionProps) {
    const [featured, ...rest] = problems ?? [];

    return (
        <DashboardSection
            title="Thử thách hôm nay"
            action={{ label: "Tất cả bài", href: ROUTES.problems }}
            className="flex-[1_1_420px]"
        >
            {problems === undefined ? <Skeleton className="h-28 rounded-lg" /> : null}
            {problems?.length === 0 ? (
                <p className="rounded-lg bg-surface-2 p-5 font-semibold">Bạn đã thử hết bài</p>
            ) : null}
            {featured ? (
                <Link
                    href={ROUTES.problem(featured.slug)}
                    className="flex items-center gap-4 rounded-lg border border-brand/40 bg-brand-soft p-4 transition-colors hover:border-brand"
                >
                    <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-brand text-sm font-extrabold text-white">
                        {`#${featured.number}`}
                    </span>
                    <span className="flex min-w-0 flex-1 flex-col gap-1">
                        <span className="truncate font-bold">{featured.title}</span>
                        <span className="flex items-center gap-2 text-sm">
                            <DifficultyLabel difficulty={featured.difficulty} />
                            {featured.tags[0] ? (
                                <span className="text-muted-foreground">
                                    {featured.tags[0].name}
                                </span>
                            ) : null}
                        </span>
                    </span>
                    <ChevronRight aria-hidden="true" className="size-5 text-muted-foreground" />
                </Link>
            ) : null}
            {rest.length > 0 ? (
                <ul className="flex flex-col divide-y">
                    {rest.map((problem) => (
                        <li key={problem.id}>
                            <Link
                                href={ROUTES.problem(problem.slug)}
                                className="flex min-h-12 items-center gap-3 py-2.5 hover:text-brand"
                            >
                                <DifficultyLabel
                                    difficulty={problem.difficulty}
                                    className="w-24 shrink-0 text-sm"
                                />
                                <span className="min-w-0 flex-1 truncate">{`#${problem.number} ${problem.title}`}</span>
                                {problem.tags[0] ? (
                                    <span className="shrink-0 rounded-full bg-surface-2 px-2.5 py-0.5 text-sm text-muted-foreground">
                                        {problem.tags[0].name}
                                    </span>
                                ) : null}
                            </Link>
                        </li>
                    ))}
                </ul>
            ) : null}
        </DashboardSection>
    );
}
