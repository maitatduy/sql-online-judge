import type { Role } from "@/lib/current-user";

interface ModerationTarget {
    username: string;
    role: Role;
}

export function isOtherAdmin(currentUsername: string | undefined, target: ModerationTarget) {
    return target.role === "ADMIN" && currentUsername !== target.username;
}

export function roleLockedReasonOf(currentUsername: string, target: ModerationTarget) {
    if (currentUsername === target.username) {
        return "Bạn không thể tự đổi vai trò của chính mình.";
    }
    return target.role === "ADMIN" ? "Không thể đổi vai trò của quản trị viên khác." : null;
}

export function canModerate(currentUsername: string | undefined, target: ModerationTarget) {
    return (
        currentUsername !== undefined &&
        currentUsername !== target.username &&
        target.role !== "ADMIN"
    );
}
