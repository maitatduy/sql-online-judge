import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";

import { UserDetailBody } from "./UserDetailBody";

interface UserDetailDialogProps {
    username: string;
    onClose: () => void;
}

export function UserDetailDialog({ username, onClose }: UserDetailDialogProps) {
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
                aria-describedby={undefined}
                className="max-h-[calc(100vh-3rem)] max-w-[520px] overflow-y-auto p-5"
            >
                <DialogTitle className="sr-only">Chi tiết người dùng</DialogTitle>
                <UserDetailBody username={username} onClose={onClose} />
            </DialogContent>
        </Dialog>
    );
}
