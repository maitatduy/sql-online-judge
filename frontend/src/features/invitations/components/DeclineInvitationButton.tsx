"use client";

import { useState } from "react";

import { DeleteConfirmDialog } from "@/components/shared/DeleteConfirmDialog";
import { Button } from "@/components/ui/button";

interface DeclineInvitationButtonProps {
    roleLabel: string;
    isPending: boolean;
    onConfirm: () => void;
}

export function DeclineInvitationButton({
    roleLabel,
    isPending,
    onConfirm,
}: DeclineInvitationButtonProps) {
    const [isOpen, setIsOpen] = useState(false);

    return (
        <>
            <Button
                type="button"
                size="lg"
                variant="ghost"
                className="h-12.5 w-full text-[15px]"
                disabled={isPending}
                onClick={() => setIsOpen(true)}
            >
                Từ chối lời mời
            </Button>
            <DeleteConfirmDialog
                open={isOpen}
                onOpenChange={setIsOpen}
                title="Từ chối lời mời?"
                description={`Bạn sẽ không nhận quyền ${roleLabel} từ lời mời này. Link sẽ không dùng được nữa.`}
                confirmLabel="Từ chối"
                isPending={isPending}
                onConfirm={() => {
                    setIsOpen(false);
                    onConfirm();
                }}
            />
        </>
    );
}
