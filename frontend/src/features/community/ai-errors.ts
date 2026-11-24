import { ApiRequestError } from "@/lib/api-client";

export function aiErrorText(error: Error): string {
    if (!(error instanceof ApiRequestError)) {
        return error.message;
    }
    if (error.code === "AI_DISABLED_DURING_CONTEST") {
        return "Trợ lý AI tạm khoá khi bạn đang thi contest tính rating.";
    }
    if (error.status === 429) {
        return "Bạn đã dùng hết lượt AI hôm nay.";
    }
    return error.message;
}
