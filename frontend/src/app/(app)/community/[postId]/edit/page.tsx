import type { Metadata } from "next";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { EditPostView } from "@/features/community/components/PostEditorView";

interface EditPostPageProps {
    params: Promise<{ postId: string }>;
}

export const metadata: Metadata = { title: "Sửa bài viết - Cộng đồng" };

export default async function EditPostPage({ params }: EditPostPageProps) {
    const { postId } = await params;
    return (
        <main id={MAIN_CONTENT_ID} className="px-4 pt-6 pb-16 md:px-6">
            <EditPostView postId={decodeURIComponent(postId)} />
        </main>
    );
}
