import { ROLE_LABELS } from "@/features/admin/users/role-labels";
import type { Role } from "@/lib/current-user";
import { cn } from "@/lib/utils";

const ROLE_CLASS_NAMES: Record<Role, string> = {
    LEARNER: "bg-surface-2 text-muted-foreground",
    SETTER: "bg-role-setter/12 text-role-setter",
    MODERATOR: "bg-role-moderator/12 text-role-moderator",
    ADMIN: "bg-role-admin/12 text-role-admin",
};

interface RoleBadgeProps {
    role: Role;
}

export function RoleBadge({ role }: RoleBadgeProps) {
    return (
        <span
            className={cn(
                "inline-flex h-5.5 items-center rounded-full px-2 text-xs font-semibold whitespace-nowrap",
                ROLE_CLASS_NAMES[role],
            )}
        >
            {ROLE_LABELS[role]}
        </span>
    );
}
