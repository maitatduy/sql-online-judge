import type { Metadata } from "next";

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
                    value: "solutions",
                    label: "Lời giải",
                    content: <ProblemSolutionsTab problemSlug={problemSlug} />,
                },
                {
                    value: "discussion",
                    label: "Thảo luận",
                    content: <ProblemDiscussionTab problemSlug={problemSlug} />,
                },
            ]}
        />
    );
}
