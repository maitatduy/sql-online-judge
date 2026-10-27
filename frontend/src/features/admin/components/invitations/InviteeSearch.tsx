import { Search } from "lucide-react";
import { useState } from "react";

import { RoleBadge } from "@/features/admin/components/users/RoleBadge";
import { useInviteCandidates } from "@/features/admin/hooks/useInviteCandidates";
import {
    ineligibilityOf,
    isInvitableEmail,
    normalizeEmail,
} from "@/features/admin/invitations/eligibility";
import type { InvitableRole } from "@/features/admin/invitations/types";
import { useDebouncedSearch } from "@/lib/useDebouncedSearch";

export interface Invitee {
    email: string;
    label: string;
}

interface InviteeSearchProps {
    role: InvitableRole;
    pendingEmails: ReadonlySet<string>;
    selectedEmails: ReadonlySet<string>;
    isFull: boolean;
    onAdd: (invitee: Invitee) => void;
}

const ROW_CLASS =
    "flex w-full flex-wrap items-center gap-x-2 gap-y-1 rounded-md px-2.5 py-2 text-left text-sm hover:bg-surface disabled:cursor-not-allowed disabled:opacity-55 disabled:hover:bg-transparent";

export function InviteeSearch({
    role,
    pendingEmails,
    selectedEmails,
    isFull,
    onAdd,
}: InviteeSearchProps) {
    const [query, setQuery] = useState("");
    const { searchText, changeSearchText } = useDebouncedSearch(query, setQuery);
    const { data } = useInviteCandidates(query);
    const candidates = query.trim().length >= 2 ? (data?.items ?? []) : [];
    const typedEmail = normalizeEmail(query);
    const canInviteTypedEmail =
        isInvitableEmail(query) &&
        !selectedEmails.has(typedEmail) &&
        !candidates.some((candidate) => candidate.email.toLowerCase() === typedEmail);

    return (
        <div className="flex flex-col gap-2">
            <label className="flex h-9 items-center gap-2 rounded-md border px-2.5 text-muted-foreground focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50">
                <Search aria-hidden="true" className="size-4 shrink-0" />
                <input
                    type="search"
                    aria-label="Tìm người dùng"
                    placeholder="Username hoặc email…"
                    value={searchText}
                    onChange={(event) => changeSearchText(event.target.value)}
                    className="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none"
                />
            </label>
            {candidates.length > 0 || canInviteTypedEmail ? (
                <ul aria-label="Gợi ý" className="flex max-h-56 flex-col overflow-y-auto">
                    {candidates.map((candidate) => {
                        const email = candidate.email.toLowerCase();
                        const reason = selectedEmails.has(email)
                            ? "Đã thêm"
                            : ineligibilityOf(candidate, role, pendingEmails);
                        return (
                            <li key={candidate.id}>
                                <button
                                    type="button"
                                    disabled={reason !== null || isFull}
                                    onClick={() => onAdd({ email, label: candidate.username })}
                                    className={ROW_CLASS}
                                >
                                    <span className="font-semibold">{candidate.username}</span>
                                    <span className="text-muted-foreground">{candidate.email}</span>
                                    <RoleBadge role={candidate.role} />
                                    {reason ? (
                                        <span className="text-xs text-muted-foreground">
                                            · {reason}
                                        </span>
                                    ) : null}
                                </button>
                            </li>
                        );
                    })}
                    {canInviteTypedEmail ? (
                        <li>
                            <button
                                type="button"
                                disabled={isFull}
                                onClick={() => onAdd({ email: typedEmail, label: typedEmail })}
                                className={ROW_CLASS}
                            >
                                Mời email này: {typedEmail}
                            </button>
                        </li>
                    ) : null}
                </ul>
            ) : null}
        </div>
    );
}
