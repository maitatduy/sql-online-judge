import { ROUTES } from "@/lib/routes";

import type { AuthMode } from "./types";

interface AuthContent {
    eyebrow: string;
    lead: string;
    sampleLearnerName: string;
    title: string;
    subtitle: string;
    switchQuestion: string;
    switchLabel: string;
    switchHref: string;
}

export const AUTH_CONTENT: Record<AuthMode, AuthContent> = {
    login: {
        eyebrow: "Chào mừng trở lại",
        lead: "Lộ trình, bài đang làm dở và chuỗi ngày học của bạn vẫn ở đây. Giải một bài hôm nay để giữ lửa.",
        sampleLearnerName: "'bạn'",
        title: "Chào mừng trở lại",
        subtitle: "Đăng nhập để tiếp tục lộ trình và bài đang làm dở.",
        switchQuestion: "Chưa có tài khoản?",
        switchLabel: "Đăng ký miễn phí",
        switchHref: ROUTES.register,
    },
    register: {
        eyebrow: "Tạo tài khoản mới",
        lead: "Bài tập có máy chấm, contest hằng tuần và trợ lý AI chỉ ra vì sao truy vấn của bạn sai. Miễn phí cho người học.",
        sampleLearnerName: "'duy'",
        title: "Tạo tài khoản miễn phí",
        subtitle: "Mất chưa tới một phút. Không cần thẻ thanh toán.",
        switchQuestion: "Đã có tài khoản?",
        switchLabel: "Đăng nhập",
        switchHref: ROUTES.login,
    },
};
