import Image from "next/image";

import { initialsOf } from "@/lib/initials";
import { cn } from "@/lib/utils";

const IMAGE_SIZE = 96;

interface AvatarProps {
    name: string;
    src?: string | null;
    className?: string;
}

export function Avatar({ name, src, className }: AvatarProps) {
    if (src) {
        return (
            <Image
                unoptimized
                src={src}
                alt=""
                width={IMAGE_SIZE}
                height={IMAGE_SIZE}
                className={cn("size-8 shrink-0 rounded-full object-cover", className)}
            />
        );
    }
    return (
        <span
            className={cn(
                "inline-flex size-8 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-bold text-primary-foreground",
                className,
            )}
        >
            {initialsOf(name)}
        </span>
    );
}
