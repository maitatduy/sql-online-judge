"use client";

import { ArrowRight } from "lucide-react";
import { useId, useState } from "react";

import { cn } from "@/lib/utils";

import { useTopicCounts } from "../hooks/useLanding";
import { ROADMAP_STAGES, type RoadmapStage } from "../roadmap-stages";
import { RoadmapStageSheet } from "./RoadmapStageSheet";
import { TopicCount } from "./TopicCount";

const STAGE_TAGS = ROADMAP_STAGES.map((stage) => stage.tag);

function halves(subtopics: string[]): [string[], string[]] {
    const middle = Math.ceil(subtopics.length / 2);
    return [subtopics.slice(0, middle), subtopics.slice(middle)];
}

interface BranchProps {
    subtopics: string[];
    side: "left" | "right";
}

function Branch({ subtopics, side }: BranchProps) {
    if (subtopics.length === 0) {
        return <div aria-hidden="true" className="max-md:hidden" />;
    }
    const isLeft = side === "left";
    return (
        <div className={cn("flex items-center max-md:hidden", isLeft ? "justify-end" : "")}>
            {isLeft ? null : (
                <span aria-hidden="true" className="w-8 border-t-2 border-dashed border-brand/40" />
            )}
            <ul
                className={cn(
                    "flex flex-col gap-2 border-dashed border-brand/40 py-1",
                    isLeft ? "border-r-2 pr-4" : "border-l-2 pl-4",
                )}
            >
                {subtopics.map((subtopic) => (
                    <li
                        key={subtopic}
                        className="rounded-md border border-brand/30 bg-brand-soft px-3 py-1.5 text-center text-sm font-semibold whitespace-nowrap"
                    >
                        {subtopic}
                    </li>
                ))}
            </ul>
            {isLeft ? (
                <span aria-hidden="true" className="w-8 border-t-2 border-dashed border-brand/40" />
            ) : null}
        </div>
    );
}

export function RoadmapChart() {
    const titleId = useId();
    const counts = useTopicCounts(STAGE_TAGS);
    const [openStage, setOpenStage] = useState<number | null>(null);

    return (
        <section aria-labelledby={titleId} className="border-y bg-surface">
            <div className="mx-auto max-w-5xl px-6 py-16 md:py-24">
                <div className="flex flex-wrap items-end justify-between gap-4">
                    <div>
                        <h2
                            id={titleId}
                            className="text-3xl font-extrabold tracking-[-0.02em] md:text-[40px]"
                        >
                            Lộ trình SQL
                        </h2>
                        <p className="mt-2 text-[17px] text-muted-foreground">
                            Bấm vào một chủ đề để xem ví dụ và luyện bài.
                        </p>
                    </div>
                    <ul aria-label="Chú thích" className="flex gap-4 text-sm text-muted-foreground">
                        <li className="flex items-center gap-2">
                            <span aria-hidden="true" className="size-3.5 rounded-sm bg-primary" />
                            Chủ đề chính
                        </li>
                        <li className="flex items-center gap-2">
                            <span
                                aria-hidden="true"
                                className="size-3.5 rounded-sm border border-brand/30 bg-brand-soft"
                            />
                            Kiến thức con
                        </li>
                    </ul>
                </div>
                <ol className="relative mt-12">
                    <span
                        aria-hidden="true"
                        className="absolute top-0 bottom-0 left-5 w-1 rounded-full bg-brand/25 md:left-1/2 md:-translate-x-1/2"
                    />
                    {ROADMAP_STAGES.map((stage, index) => {
                        const [left, right] = halves(stage.subtopics);
                        return (
                            <li
                                key={stage.tag}
                                className="relative grid items-center gap-x-0 gap-y-3 py-5 max-md:pl-12 md:grid-cols-[1fr_auto_1fr]"
                            >
                                <Branch subtopics={left} side="left" />
                                <StageNode
                                    stage={stage}
                                    index={index}
                                    count={counts[index]}
                                    onOpen={() => setOpenStage(index)}
                                />
                                <Branch subtopics={right} side="right" />
                                <ul className="flex flex-wrap gap-2 md:hidden">
                                    {stage.subtopics.map((subtopic) => (
                                        <li
                                            key={subtopic}
                                            className="rounded-md border border-brand/30 bg-brand-soft px-2.5 py-1 text-[13px] font-semibold"
                                        >
                                            {subtopic}
                                        </li>
                                    ))}
                                </ul>
                            </li>
                        );
                    })}
                </ol>
            </div>
            {openStage === null ? null : (
                <RoadmapStageSheet
                    stage={ROADMAP_STAGES[openStage]}
                    index={openStage}
                    count={counts[openStage]}
                    onClose={() => setOpenStage(null)}
                />
            )}
        </section>
    );
}

interface StageNodeProps {
    stage: RoadmapStage;
    index: number;
    count: number | undefined;
    onOpen: () => void;
}

function StageNode({ stage, index, count, onOpen }: StageNodeProps) {
    return (
        <div className="relative md:w-64">
            <span
                aria-hidden="true"
                className="absolute top-1/2 -left-[34px] size-4 -translate-y-1/2 rounded-full border-4 border-surface bg-primary md:hidden"
            />
            <button
                type="button"
                onClick={onOpen}
                aria-haspopup="dialog"
                className="group flex w-full items-center gap-3 rounded-xl bg-primary px-4 py-3.5 text-left text-primary-foreground shadow-lg shadow-brand/20 transition-transform outline-none hover:-translate-y-0.5 focus-visible:ring-[3px] focus-visible:ring-ring/50"
            >
                <span className="text-sm font-extrabold opacity-70">
                    {String(index + 1).padStart(2, "0")}
                </span>
                <span className="min-w-0 flex-1">
                    <span className="block font-extrabold">{stage.title}</span>
                    <TopicCount count={count} className="text-[13px] opacity-80" />
                </span>
                <ArrowRight
                    aria-hidden="true"
                    className="size-4 shrink-0 transition-transform group-hover:translate-x-0.5"
                />
            </button>
        </div>
    );
}
