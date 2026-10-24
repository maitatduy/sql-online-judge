import { useEffect, useRef, useState } from "react";

export const SEARCH_DEBOUNCE_MS = 300;

export function useDebouncedSearch(appliedQuery: string, onSearch: (query: string) => void) {
    const [searchText, setSearchText] = useState(appliedQuery);
    const [syncedQuery, setSyncedQuery] = useState(appliedQuery);
    const pendingSearch = useRef<ReturnType<typeof setTimeout>>(undefined);

    if (appliedQuery !== syncedQuery) {
        setSyncedQuery(appliedQuery);
        if (appliedQuery !== searchText.trim()) {
            setSearchText(appliedQuery);
        }
    }

    useEffect(() => () => clearTimeout(pendingSearch.current), []);

    function changeSearchText(nextText: string) {
        setSearchText(nextText);
        clearTimeout(pendingSearch.current);
        pendingSearch.current = setTimeout(() => onSearch(nextText.trim()), SEARCH_DEBOUNCE_MS);
    }

    function clearSearchText() {
        clearTimeout(pendingSearch.current);
        setSearchText("");
    }

    return { searchText, changeSearchText, clearSearchText };
}
