import { BookOpenCheck, MessagesSquare, Sparkles } from "lucide-react";
import type { Metadata } from "next";

import { ProblemAssistantTab } from "@/features/ai/components/ProblemAssistantTab";
import { ProblemDiscussionTab } from "@/features/community/components/ProblemDiscussionTab";
import { ProblemSolutionsTab } from "@/features/community/components/ProblemSolutionsTab";
import { Workspace } from "@/features/practice/components/Workspace";

export const metadata: Metadata = {
    title: "Làm bài",
};

interface WorkspacePageProps {
    params: Promise<{ slug: string }>;
}

export default async function WorkspacePage({ params }: WorkspacePageProps) {
    const { slug } = await params;
    const problemSlug = decodeURIComponent(slug);
    return (
        <Workspace
            slug={slug}
            extraTabs={[
                {
                    value: "assistant",
                    label: "Trợ lý AI",
                    icon: <Sparkles aria-hidden="true" className="text-ai" />,
                    content: <ProblemAssistantTab problemSlug={problemSlug} />,
                },
                {
                    value: "solutions",
                    label: "Lời giải",
                    icon: <BookOpenCheck aria-hidden="true" />,
                    content: <ProblemSolutionsTab problemSlug={problemSlug} />,
                },
                {
                    value: "discussion",
                    label: "Thảo luận",
                    icon: <MessagesSquare aria-hidden="true" />,
                    content: <ProblemDiscussionTab problemSlug={problemSlug} />,
                },
            ]}
        />
    );
}
