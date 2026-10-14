import { useCallback, useState } from "react";

import { toggleOne, togglePage as togglePageSelection } from "../users/selection";

export interface UserSelection {
    selected: ReadonlySet<string>;
    toggleUser: (username: string) => void;
    togglePage: (pageUsernames: readonly string[]) => void;
    clearSelection: () => void;
}

export function useUserSelection(): UserSelection {
    const [selected, setSelected] = useState<ReadonlySet<string>>(() => new Set());

    const toggleUser = useCallback(
        (username: string) => setSelected((current) => toggleOne(current, username)),
        [],
    );
    const togglePage = useCallback(
        (pageUsernames: readonly string[]) =>
            setSelected((current) => togglePageSelection(current, pageUsernames)),
        [],
    );
    const clearSelection = useCallback(() => setSelected(new Set()), []);

    return { selected, toggleUser, togglePage, clearSelection };
}
