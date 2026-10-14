import { useState } from "react";

import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";

interface ActionDialogProps {
    title: string;
    description: React.ReactNode;
    onClose: () => void;
    children: React.ReactNode;
}

export function ActionDialog({ title, description, onClose, children }: ActionDialogProps) {
    const [opener] = useState(() => document.activeElement);

    return (
        <Dialog
            open
            onOpenChange={(isOpen) => {
                if (!isOpen) {
                    onClose();
                }
            }}
        >
            <DialogContent
                className="max-w-[480px]"
                onCloseAutoFocus={(event) => {
                    event.preventDefault();
                    if (opener instanceof HTMLElement && opener.isConnected) {
                        opener.focus();
                    }
                }}
            >
                <div>
                    <DialogTitle>{title}</DialogTitle>
                    <DialogDescription>{description}</DialogDescription>
                </div>
                {children}
            </DialogContent>
        </Dialog>
    );
}
