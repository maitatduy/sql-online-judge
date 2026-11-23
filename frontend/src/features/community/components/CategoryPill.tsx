import { cn } from "@/lib/utils";

import { POST_CATEGORY_CLASSES, POST_CATEGORY_LABELS } from "../labels";
import type { PostCategory } from "../types";

interface CategoryPillProps {
    category: PostCategory;
}

export function CategoryPill({ category }: CategoryPillProps) {
    return (
        <span
            className={cn(
                "inline-flex h-6 items-center rounded-sm px-2 text-xs font-semibold",
                POST_CATEGORY_CLASSES[category],
            )}
        >
            {POST_CATEGORY_LABELS[category]}
        </span>
    );
}
