"use client";

import { Skeleton } from "@/components/shared/Skeleton";

import { useLinkedProblem } from "../hooks/useCommunity";
import { CommentSection } from "./CommentSection";

interface ProblemDiscussionTabProps {
    problemSlug: string;
}

export function ProblemDiscussionTab({ problemSlug }: ProblemDiscussionTabProps) {
    const { data: problem } = useLinkedProblem(problemSlug);

    return (
        <div className="p-5">
            {problem ? (
                <CommentSection
                    thread={{ problemId: problem.id }}
                    placeholder="Hỏi hoặc gợi ý về bài này… không đăng lời giải trọn vẹn"
                />
            ) : (
                <Skeleton className="h-24" />
            )}
        </div>
    );
}
