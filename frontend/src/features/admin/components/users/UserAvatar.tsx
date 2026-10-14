import Image from "next/image";

import { Avatar } from "@/components/shared/Avatar";
import { cn } from "@/lib/utils";

const AVATAR_CLASS_NAMES = [
    "bg-avatar-1",
    "bg-avatar-2",
    "bg-avatar-3",
    "bg-avatar-4",
    "bg-avatar-5",
    "bg-avatar-6",
    "bg-avatar-7",
    "bg-avatar-8",
];
const USERNAME_SEPARATORS = /[._-]+/g;
const AVATAR_IMAGE_SIZE = 320;

function avatarClassNameOf(username: string): string {
    const charCodeSum = Array.from(username).reduce(
        (sum, character) => sum + character.charCodeAt(0),
        0,
    );
    return AVATAR_CLASS_NAMES[charCodeSum % AVATAR_CLASS_NAMES.length];
}

interface UserAvatarProps {
    username: string;
    avatarUrl?: string | null;
    label?: string;
    className?: string;
}

export function UserAvatar({ username, avatarUrl, label, className }: UserAvatarProps) {
    const sizeClassName = cn("size-7.5 text-[11px]", className);

    if (avatarUrl) {
        return (
            <Image
                unoptimized
                width={AVATAR_IMAGE_SIZE}
                height={AVATAR_IMAGE_SIZE}
                src={avatarUrl}
                alt={label ?? ""}
                className={cn("shrink-0 rounded-full object-cover", sizeClassName)}
            />
        );
    }

    return (
        <span aria-hidden="true" className="inline-flex shrink-0">
            <Avatar
                name={username.replace(USERNAME_SEPARATORS, " ")}
                className={cn(avatarClassNameOf(username), sizeClassName)}
            />
        </span>
    );
}
