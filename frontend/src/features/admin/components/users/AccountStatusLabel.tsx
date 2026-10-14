import { ACCOUNT_STATE_LABELS, type AccountState } from "@/features/admin/users/account-status";
import { cn } from "@/lib/utils";

interface StateAppearance {
    textClassName: string;
    dotClassName: string;
}

const STATE_APPEARANCES: Record<AccountState, StateAppearance> = {
    ACTIVE: { textClassName: "text-account-active", dotClassName: "bg-account-active" },
    BANNED: { textClassName: "text-account-banned", dotClassName: "bg-account-banned" },
    UNVERIFIED: {
        textClassName: "text-account-unverified",
        dotClassName: "bg-account-unverified",
    },
};

interface AccountStatusLabelProps {
    state: AccountState;
}

export function AccountStatusLabel({ state }: AccountStatusLabelProps) {
    const { textClassName, dotClassName } = STATE_APPEARANCES[state];

    return (
        <span
            className={cn(
                "inline-flex items-center gap-1.5 font-semibold whitespace-nowrap",
                textClassName,
            )}
        >
            <span
                aria-hidden="true"
                className={cn("size-1.75 shrink-0 rounded-full", dotClassName)}
            />
            {ACCOUNT_STATE_LABELS[state]}
        </span>
    );
}
