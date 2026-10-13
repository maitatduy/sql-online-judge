import { initialsOf } from "@/lib/initials";
import { cn } from "@/lib/utils";

interface AvatarProps {
    name: string;
    className?: string;
}

export function Avatar({ name, className }: AvatarProps) {
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
