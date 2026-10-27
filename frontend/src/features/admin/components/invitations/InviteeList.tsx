import { X } from "lucide-react";

import { Button } from "@/components/ui/button";

import type { Invitee } from "./InviteeSearch";

interface InviteeListProps {
    invitees: Invitee[];
    onRemove: (email: string) => void;
}

export function InviteeList({ invitees, onRemove }: InviteeListProps) {
    return (
        <ul aria-label="Người sẽ được mời" className="flex min-h-9 flex-wrap gap-1.5">
            {invitees.length === 0 ? (
                <li className="text-[13px] text-muted-foreground">Chưa chọn ai.</li>
            ) : null}
            {invitees.map((invitee) => (
                <li
                    key={invitee.email}
                    className="inline-flex h-8 items-center gap-1 rounded-full bg-surface-2 pr-1 pl-3 text-[13px] font-semibold"
                >
                    {invitee.label}
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        aria-label={`Bỏ ${invitee.label}`}
                        className="size-6 rounded-full"
                        onClick={() => onRemove(invitee.email)}
                    >
                        <X aria-hidden="true" />
                    </Button>
                </li>
            ))}
        </ul>
    );
}
