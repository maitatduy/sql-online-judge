import { Expand } from "lucide-react";
import { useState } from "react";

import { AvatarPreviewDialog } from "./AvatarPreviewDialog";
import { UserAvatar } from "./UserAvatar";

interface DetailAvatarProps {
    username: string;
    avatarUrl: string | null;
}

export function DetailAvatar({ username, avatarUrl }: DetailAvatarProps) {
    const [isPreviewOpen, setIsPreviewOpen] = useState(false);

    return (
        <div className="relative shrink-0">
            <UserAvatar
                username={username}
                avatarUrl={avatarUrl}
                label={`Ảnh đại diện của ${username}`}
                className="size-13 text-lg"
            />
            {avatarUrl ? (
                <>
                    <button
                        type="button"
                        aria-label="Xem ảnh đại diện"
                        onClick={() => setIsPreviewOpen(true)}
                        className="absolute -right-1 -bottom-1 flex size-6 items-center justify-center rounded-full border bg-card text-muted-foreground shadow-sm hover:text-foreground"
                    >
                        <Expand aria-hidden="true" className="size-3" />
                    </button>
                    {isPreviewOpen ? (
                        <AvatarPreviewDialog
                            username={username}
                            avatarUrl={avatarUrl}
                            onClose={() => setIsPreviewOpen(false)}
                        />
                    ) : null}
                </>
            ) : null}
        </div>
    );
}
