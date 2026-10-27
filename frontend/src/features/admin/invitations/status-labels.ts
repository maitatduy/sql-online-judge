import type { InvitationStatus } from "./types";

export const INVITATION_STATUS_LABELS: Record<InvitationStatus, string> = {
    PENDING: "Đang chờ",
    ACCEPTED: "Đã nhận",
    EXPIRED: "Hết hạn",
    REVOKED: "Đã huỷ",
};
