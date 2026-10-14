import type { Role } from "@/lib/current-user";

export const ROLE_LABELS: Record<Role, string> = {
    LEARNER: "Học viên",
    SETTER: "Người ra đề",
    MODERATOR: "Kiểm duyệt",
    ADMIN: "Quản trị",
};

export const ROLES = Object.keys(ROLE_LABELS) as Role[];
