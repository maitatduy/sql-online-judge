import Link from "next/link";

import { Skeleton } from "@/components/shared/Skeleton";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import type { TopicProgress } from "../../types";
import { DashboardSection } from "./DashboardSection";

interface TopicSkillsSectionProps {
    topics: TopicProgress[] | undefined;
}

const SHOWN_TOPICS = 6;
const FULL_PERCENT = 100;

export function TopicSkillsSection({ topics }: TopicSkillsSectionProps) {
    const shown = topics?.slice(0, SHOWN_TOPICS);

    return (
        <DashboardSection
            title="Kỹ năng theo chủ đề"
            action={{ label: "Luyện tập", href: ROUTES.problems }}
            className="flex-[1_1_320px]"
        >
            {shown === undefined ? <Skeleton className="h-40 rounded-lg" /> : null}
            {shown?.length === 0 ? (
                <p className="rounded-lg bg-surface-2 p-5 text-muted-foreground">
                    Chưa có chủ đề nào.
                </p>
            ) : null}
            {shown && shown.length > 0 ? (
                <ul className="flex flex-col gap-3.5">
                    {shown.map((topic) => (
                        <li key={topic.slug} className="flex flex-col gap-1.5">
                            <div className="flex items-baseline justify-between gap-3">
                                <Link
                                    href={`${ROUTES.problems}?tag=${encodeURIComponent(topic.slug)}`}
                                    className="truncate font-medium hover:text-brand"
                                >
                                    {topic.name}
                                </Link>
                                <span className="shrink-0 text-sm text-muted-foreground">
                                    {`${formatNumber(topic.solved)}/${formatNumber(topic.total)}`}
                                </span>
                            </div>
                            <div
                                role="progressbar"
                                aria-label={`${topic.name}: đã AC ${topic.solved} trên ${topic.total} bài`}
                                aria-valuemin={0}
                                aria-valuemax={topic.total}
                                aria-valuenow={topic.solved}
                                className="h-2 overflow-hidden rounded-full bg-surface-2"
                            >
                                <div
                                    className="h-full rounded-full bg-brand"
                                    style={{
                                        width: `${topic.total === 0 ? 0 : (topic.solved / topic.total) * FULL_PERCENT}%`,
                                    }}
                                />
                            </div>
                        </li>
                    ))}
                </ul>
            ) : null}
        </DashboardSection>
    );
}
