"use client";

import { Plus } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import { useCommunityOverview } from "../hooks/useCommunity";
import { POST_CATEGORIES, POST_CATEGORY_LABELS } from "../labels";
import { communityHref, DEFAULT_POST_FILTERS } from "../search-params";
import type { PostCategory } from "../types";

const LINK_CLASS =
    "flex min-h-10 items-center justify-between rounded-md px-3 text-[15px] hover:bg-surface";

interface CategoryNavProps {
    active: PostCategory | null;
}

export function CategoryNav({ active }: CategoryNavProps) {
    const { data: overview } = useCommunityOverview();
    const countOf = (category: PostCategory | null) => {
        if (!overview) {
            return "";
        }
        return formatNumber(
            category ? (overview.categoryCounts[category] ?? 0) : overview.totalPosts,
        );
    };
    const entries: (PostCategory | null)[] = [null, ...POST_CATEGORIES];

    return (
        <nav aria-label="Chuyên mục" className="flex flex-col gap-1">
            <Button asChild className="mb-3">
                <Link href={ROUTES.communityNew}>
                    <Plus aria-hidden="true" />
                    Viết bài
                </Link>
            </Button>
            {entries.map((category) => (
                <Link
                    key={category ?? "all"}
                    href={communityHref({ ...DEFAULT_POST_FILTERS, category })}
                    aria-current={active === category ? "page" : undefined}
                    className={cn(
                        LINK_CLASS,
                        active === category ? "bg-brand-soft font-semibold text-brand" : null,
                    )}
                >
                    {category ? POST_CATEGORY_LABELS[category] : "Tất cả"}
                    <span className="text-sm text-muted-foreground">{countOf(category)}</span>
                </Link>
            ))}
        </nav>
    );
}
