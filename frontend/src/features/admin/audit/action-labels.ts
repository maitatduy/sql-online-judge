import type { AuditAction } from "./types";

export const AUDIT_ACTION_LABELS: Record<AuditAction, string> = {
    BAN: "Khoá tài khoản",
    UNBAN: "Mở khoá tài khoản",
    UPDATE_ROLE: "Đổi vai trò",
    UPDATE_DISPLAY_NAME: "Đổi tên hiển thị",
    UPDATE_RESTRICTIONS: "Đổi hạn chế",
    DELETE: "Xoá tài khoản",
    RESTORE: "Khôi phục tài khoản",
    REVOKE_SESSION: "Đăng xuất một phiên",
    REVOKE_ALL_SESSIONS: "Đăng xuất mọi phiên",
    SEND_PASSWORD_RESET: "Gửi email đặt lại mật khẩu",
    SEND_VERIFICATION: "Gửi email xác minh",
    UPDATE_AVATAR: "Đổi ảnh đại diện",
    REMOVE_AVATAR: "Xoá ảnh đại diện",
    EXPORT_USER_EMAILS: "Xuất email người dùng",
};

export const AUDIT_ACTIONS = Object.keys(AUDIT_ACTION_LABELS) as AuditAction[];

const DESTRUCTIVE_ACTIONS: ReadonlySet<AuditAction> = new Set(["BAN", "DELETE"]);

export function isDestructiveAction(action: AuditAction): boolean {
    return DESTRUCTIVE_ACTIONS.has(action);
}
