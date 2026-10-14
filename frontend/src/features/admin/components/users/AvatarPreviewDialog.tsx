import Image from "next/image";

import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";

const AVATAR_IMAGE_SIZE = 320;

interface AvatarPreviewDialogProps {
    username: string;
    avatarUrl: string;
    onClose: () => void;
}

export function AvatarPreviewDialog({ username, avatarUrl, onClose }: AvatarPreviewDialogProps) {
    const label = `Ảnh đại diện của ${username}`;

    return (
        <Dialog
            open
            onOpenChange={(isOpen) => {
                if (!isOpen) {
                    onClose();
                }
            }}
        >
            <DialogContent aria-describedby={undefined} className="max-w-[360px] items-center">
                <DialogTitle className="text-base">{label}</DialogTitle>
                <Image
                    unoptimized
                    width={AVATAR_IMAGE_SIZE}
                    height={AVATAR_IMAGE_SIZE}
                    src={avatarUrl}
                    alt={label}
                    className="aspect-square w-full max-w-80 rounded-lg object-cover"
                />
            </DialogContent>
        </Dialog>
    );
}
