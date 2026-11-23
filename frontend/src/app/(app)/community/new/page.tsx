import type { Metadata } from "next";
import { Suspense } from "react";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { NewPostView } from "@/features/community/components/PostEditorView";

export const metadata: Metadata = { title: "Viết bài - Cộng đồng" };

export default function NewPostPage() {
    return (
        <main id={MAIN_CONTENT_ID} className="px-4 pt-6 pb-16 md:px-6">
            <Suspense>
                <NewPostView />
            </Suspense>
        </main>
    );
}
