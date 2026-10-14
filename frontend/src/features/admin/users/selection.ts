export type PageSelection = "none" | "some" | "all";

export function toggleOne(selected: ReadonlySet<string>, username: string): Set<string> {
    const nextSelected = new Set(selected);
    if (!nextSelected.delete(username)) {
        nextSelected.add(username);
    }
    return nextSelected;
}

export function pageSelectionOf(
    selected: ReadonlySet<string>,
    pageUsernames: readonly string[],
): PageSelection {
    const selectedOnPage = pageUsernames.filter((username) => selected.has(username)).length;
    if (selectedOnPage === 0) {
        return "none";
    }
    return selectedOnPage === pageUsernames.length ? "all" : "some";
}

export function togglePage(
    selected: ReadonlySet<string>,
    pageUsernames: readonly string[],
): Set<string> {
    const nextSelected = new Set(selected);
    const isPageSelected = pageSelectionOf(selected, pageUsernames) === "all";
    pageUsernames.forEach((username) =>
        isPageSelected ? nextSelected.delete(username) : nextSelected.add(username),
    );
    return nextSelected;
}
