import { cn } from "@/lib/utils";

import type { RoadmapLevel, RoadmapStage } from "../types";

import { SectionHeading } from "./SectionHeading";

interface RoadmapSectionProps {
    stages: RoadmapStage[];
}

const LEVEL_CLASSES: Record<RoadmapLevel, string> = {
    FOUNDATION: "border-t-brand",
    ADVANCED: "border-t-brand/50",
};

export function RoadmapSection({ stages }: RoadmapSectionProps) {
    return (
        <section className="border-y bg-surface">
            <div className="mx-auto max-w-7xl px-6 py-16 md:py-24">
                <SectionHeading
                    eyebrow="Lộ trình mẫu"
                    title="Sáu chặng, đi theo tốc độ của bạn"
                    className="mb-10"
                />
                <ol className="grid grid-cols-[repeat(auto-fit,minmax(170px,1fr))] gap-3">
                    {stages.map((stage, index) => (
                        <li
                            key={stage.title}
                            className={cn(
                                "rounded-lg border border-t-[3px] bg-card p-4.5",
                                LEVEL_CLASSES[stage.level],
                            )}
                        >
                            <span className="font-mono text-xs text-muted-foreground">
                                {String(index + 1).padStart(2, "0")}
                            </span>
                            <p className="mt-1.5 mb-0.5 font-bold">{stage.title}</p>
                            <p className="text-[13px] text-muted-foreground">{stage.topics}</p>
                        </li>
                    ))}
                </ol>
            </div>
        </section>
    );
}
