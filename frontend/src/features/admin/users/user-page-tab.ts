export const USER_PAGE_TABS = ["overview", "contests", "violations", "sessions"] as const;

export type UserPageTab = (typeof USER_PAGE_TABS)[number];

const TAB_PARAM = "tab";
const DEFAULT_TAB: UserPageTab = "overview";

export function readUserPageTab(searchParams: Pick<URLSearchParams, "get">): UserPageTab {
    const tab = searchParams.get(TAB_PARAM);
    return USER_PAGE_TABS.find((knownTab) => knownTab === tab) ?? DEFAULT_TAB;
}

export function userPageTabSearch(tab: UserPageTab): string {
    return tab === DEFAULT_TAB ? "" : `?${TAB_PARAM}=${tab}`;
}
