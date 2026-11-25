"use client";

import { ArrowRight } from "lucide-react";
import Link from "next/link";
import { useId, useRef, useState } from "react";

import { EditorPreview } from "@/components/shared/EditorPreview";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import { useTopicCounts } from "../hooks/useLanding";
import { ROADMAP_STAGES } from "../roadmap-stages";

const STAGE_TAGS = ROADMAP_STAGES.map((stage) => stage.tag);
const LAST_INDEX = ROADMAP_STAGES.length - 1;

function stageNumber(index: number): string {
    return String(index + 1).padStart(2, "0");
}

export function QueryTrack() {
    const baseId = useId();
    const [activeIndex, setActiveIndex] = useState(0);
    const tabs = useRef<(HTMLButtonElement | null)[]>([]);
    const counts = useTopicCounts(STAGE_TAGS);
    const stage = ROADMAP_STAGES[activeIndex];
    const count = counts[activeIndex];
    const progressPercent = (activeIndex / LAST_INDEX) * 100;

    function select(index: number) {
        setActiveIndex(index);
        tabs.current[index]?.focus();
    }

    function handleKeyDown(event: React.KeyboardEvent) {
        const moves: Record<string, number> = {
            ArrowRight: Math.min(LAST_INDEX, activeIndex + 1),
            ArrowLeft: Math.max(0, activeIndex - 1),
            Home: 0,
            End: LAST_INDEX,
        };
        if (event.key in moves) {
            event.preventDefault();
            select(moves[event.key]);
        }
    }

    return (
        <section aria-labelledby={`${baseId}-title`} className="border-y bg-surface">
            <div className="mx-auto max-w-7xl px-6 py-16 md:py-24">
                <h2
                    id={`${baseId}-title`}
                    className="text-3xl font-extrabold tracking-[-0.02em] md:text-[40px]"
                >
                    Từ <span className="text-brand">SELECT</span> đến{" "}
                    <span className="text-brand">OVER()</span>
                </h2>
                <p className="mt-2 text-[17px] text-muted-foreground">
                    Sáu trạm, đi theo tốc độ của bạn.
                </p>
                <div className="-mx-6 mt-10 overflow-x-auto px-6 pb-2">
                    <div className="relative min-w-[720px]">
                        <div
                            aria-hidden="true"
                            className="absolute top-[11px] right-[8.33%] left-[8.33%] h-1 rounded-full bg-border"
                        >
                            <div
                                className="h-full rounded-full bg-brand transition-[width] duration-500 ease-out"
                                style={{ width: `${progressPercent}%` }}
                            />
                        </div>
                        <div
                            role="tablist"
                            aria-label="Các trạm của lộ trình"
                            onKeyDown={handleKeyDown}
                            className="relative grid grid-cols-6"
                        >
                            {ROADMAP_STAGES.map((item, index) => {
                                const isActive = index === activeIndex;
                                const isPassed = index <= activeIndex;
                                return (
                                    <button
                                        key={item.tag}
                                        ref={(element) => {
                                            tabs.current[index] = element;
                                        }}
                                        type="button"
                                        role="tab"
                                        id={`${baseId}-tab-${index}`}
                                        aria-selected={isActive}
                                        aria-controls={`${baseId}-panel`}
                                        tabIndex={isActive ? 0 : -1}
                                        onClick={() => select(index)}
                                        className="group flex flex-col items-center gap-3 rounded-lg px-1 pb-2 outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
                                    >
                                        <span
                                            aria-hidden="true"
                                            className={cn(
                                                "size-[26px] rounded-full border-4 bg-card transition-all duration-300",
                                                isPassed ? "border-brand" : "border-border",
                                                isActive && "scale-125 bg-brand",
                                            )}
                                        />
                                        <span
                                            className={cn(
                                                "text-[15px] font-extrabold tracking-[0.02em] whitespace-nowrap transition-colors",
                                                isActive
                                                    ? "text-brand"
                                                    : "text-muted-foreground group-hover:text-foreground",
                                            )}
                                        >
                                            {item.keyword}
                                        </span>
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                </div>
                <div
                    role="tabpanel"
                    id={`${baseId}-panel`}
                    aria-labelledby={`${baseId}-tab-${activeIndex}`}
                    className="mt-8 grid items-center gap-8 lg:grid-cols-[2fr_3fr]"
                >
                    <div>
                        <p className="text-[56px] leading-none font-extrabold text-brand/25">
                            {stageNumber(activeIndex)}
                        </p>
                        <h3 className="mt-2 text-2xl font-extrabold">{stage.title}</h3>
                        <div className="mt-2 text-muted-foreground">
                            {count === undefined ? (
                                <Skeleton className="h-5 w-32" />
                            ) : (
                                `${formatNumber(count)} bài có máy chấm`
                            )}
                        </div>
                        <Button asChild className="mt-6">
                            <Link href={`${ROUTES.problems}?tag=${encodeURIComponent(stage.tag)}`}>
                                {count ? `Luyện ${formatNumber(count)} bài` : "Xem bài tập"}
                                <ArrowRight aria-hidden="true" />
                            </Link>
                        </Button>
                    </div>
                    <EditorPreview
                        key={stage.tag}
                        label={`Câu truy vấn mẫu cho trạm ${stage.keyword}`}
                        fileName={stage.fileName}
                        verdictDetail={stage.runtime}
                        lineCount={stage.lineCount}
                        columns={stage.columns}
                        rows={stage.rows}
                    >
                        {stage.sql}
                    </EditorPreview>
                </div>
            </div>
        </section>
    );
}
