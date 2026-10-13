"use client";

import { Hourglass } from "lucide-react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Tabs } from "radix-ui";

import { EmptyState } from "@/components/shared/EmptyState";
import { formatNumber } from "@/lib/format";

import type { ProfileTabCounts } from "../types";

import { RecentSubmissionsTable } from "./RecentSubmissionsTable";

const TAB_PARAM = "tab";
const DEFAULT_TAB = "submissions";

interface ProfileTabsProps {
    username: string;
    tabCounts: ProfileTabCounts;
}

export function ProfileTabs({ username, tabCounts }: ProfileTabsProps) {
    const pathname = usePathname();
    const router = useRouter();
    const searchParams = useSearchParams();

    const upcomingTabs = [
        {
            value: "solutions",
            label: `Lời giải đã chia sẻ · ${formatNumber(tabCounts.sharedSolutions)}`,
            description: "Lời giải đã chia sẻ sẽ hiện ở đây.",
        },
        {
            value: "posts",
            label: `Bài viết · ${formatNumber(tabCounts.posts)}`,
            description: "Bài viết trong cộng đồng sẽ hiện ở đây.",
        },
        {
            value: "contests",
            label: `Contest đã thi · ${formatNumber(tabCounts.contests)}`,
            description: "Kết quả các contest đã thi sẽ hiện ở đây.",
        },
    ];
    const tabs = [{ value: DEFAULT_TAB, label: "Bài nộp gần đây" }, ...upcomingTabs];
    const requestedTab = searchParams.get(TAB_PARAM);
    const openTab = tabs.some((tab) => tab.value === requestedTab) ? requestedTab : DEFAULT_TAB;

    function handleTabChange(tab: string) {
        const nextParams = new URLSearchParams(searchParams.toString());
        if (tab === DEFAULT_TAB) {
            nextParams.delete(TAB_PARAM);
        } else {
            nextParams.set(TAB_PARAM, tab);
        }
        const query = nextParams.toString();
        router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
    }

    return (
        <Tabs.Root
            value={openTab ?? DEFAULT_TAB}
            onValueChange={handleTabChange}
            className="min-w-0 overflow-hidden rounded-lg border bg-card"
        >
            <Tabs.List
                aria-label="Hoạt động của người dùng"
                className="flex gap-0.5 overflow-x-auto border-b px-3"
            >
                {tabs.map((tab) => (
                    <Tabs.Trigger
                        key={tab.value}
                        value={tab.value}
                        className="-mb-px inline-flex min-h-11 shrink-0 items-center border-b-2 border-transparent px-3 text-[13px] font-medium whitespace-nowrap text-muted-foreground hover:text-foreground data-[state=active]:border-brand data-[state=active]:font-semibold data-[state=active]:text-foreground"
                    >
                        {tab.label}
                    </Tabs.Trigger>
                ))}
            </Tabs.List>
            <Tabs.Content value={DEFAULT_TAB}>
                <RecentSubmissionsTable username={username} />
            </Tabs.Content>
            {upcomingTabs.map((tab) => (
                <Tabs.Content key={tab.value} value={tab.value}>
                    <EmptyState
                        icon={<Hourglass />}
                        title="Sắp ra mắt"
                        description={tab.description}
                        className="m-4"
                    />
                </Tabs.Content>
            ))}
        </Tabs.Root>
    );
}
