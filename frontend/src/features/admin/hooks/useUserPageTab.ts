import { useSearchParams } from "next/navigation";

import { readUserPageTab, userPageTabSearch, type UserPageTab } from "../users/user-page-tab";

function selectTab(tab: UserPageTab): void {
    window.history.replaceState(null, "", `${window.location.pathname}${userPageTabSearch(tab)}`);
}

export function useUserPageTab() {
    return { tab: readUserPageTab(useSearchParams()), selectTab };
}
