import type { Metadata } from "next";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { PostDetailView } from "@/features/community/components/PostDetailView";

interface PostPageProps {
    params: Promise<{ postId: string }>;
}

export const metadata: Metadata = { title: "Bài viết - Cộng đồng" };

export default async function PostPage({ params }: PostPageProps) {
    const { postId } = await params;
    return (
        <main id={MAIN_CONTENT_ID} className="px-4 pt-6 pb-16 md:px-6">
            <PostDetailView postId={decodeURIComponent(postId)} />
        </main>
    );
}
