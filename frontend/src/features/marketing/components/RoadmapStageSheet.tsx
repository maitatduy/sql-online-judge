"use client";

import { ArrowRight } from "lucide-react";
import Link from "next/link";

import { EditorPreview } from "@/components/shared/EditorPreview";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import type { RoadmapStage } from "../roadmap-stages";
import { TopicCount } from "./TopicCount";

interface RoadmapStageSheetProps {
    stage: RoadmapStage;
    index: number;
    count: number | undefined;
    onClose: () => void;
}

export function RoadmapStageSheet({ stage, index, count, onClose }: RoadmapStageSheetProps) {
    return (
        <Dialog open onOpenChange={(isOpen) => (isOpen ? undefined : onClose())}>
            <DialogContent className="top-0 right-0 left-auto h-dvh max-h-dvh w-full max-w-[560px] translate-x-0 translate-y-0 gap-5 rounded-none border-y-0 border-r-0">
                <div>
                    <p className="text-sm font-extrabold text-brand">
                        {`Chủ đề ${String(index + 1).padStart(2, "0")}`}
                    </p>
                    <DialogTitle className="mt-1 text-2xl font-extrabold">
                        {stage.title}
                    </DialogTitle>
                    <DialogDescription className="mt-2 text-base">
                        {stage.summary}
                    </DialogDescription>
                </div>
                <ul aria-label="Kiến thức con" className="flex flex-wrap gap-2">
                    {stage.subtopics.map((subtopic) => (
                        <li
                            key={subtopic}
                            className="rounded-md border border-brand/30 bg-brand-soft px-2.5 py-1 text-sm font-semibold"
                        >
                            {subtopic}
                        </li>
                    ))}
                </ul>
                <EditorPreview
                    label={`Câu truy vấn mẫu cho chủ đề ${stage.title}`}
                    fileName={stage.fileName}
                    verdictDetail={stage.runtime}
                    lineCount={stage.lineCount}
                    columns={stage.columns}
                    rows={stage.rows}
                >
                    {stage.sql}
                </EditorPreview>
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <TopicCount count={count} className="text-muted-foreground" />
                    <Button asChild>
                        <Link href={`${ROUTES.problems}?tag=${encodeURIComponent(stage.tag)}`}>
                            {count ? `Luyện ${formatNumber(count)} bài` : "Xem bài tập"}
                            <ArrowRight aria-hidden="true" />
                        </Link>
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    );
}
