import type { ApiFieldError } from "@/lib/api-client";

import { apiError } from "./api-error";

const MAX_REASON_LENGTH = 500;

export function validationFailed(fieldError: ApiFieldError) {
    return apiError(400, "VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ", {
        errors: [fieldError],
    });
}

export function userNotFound() {
    return apiError(404, "USER_NOT_FOUND", "Không tìm thấy người dùng");
}

export function cannotModifySelf(message: string) {
    return apiError(422, "CANNOT_MODIFY_SELF", message);
}

export function reasonFieldError(reason: unknown): ApiFieldError | null {
    if (typeof reason !== "string" || reason.trim() === "") {
        return { field: "reason", message: "Không được để trống" };
    }
    return reason.length > MAX_REASON_LENGTH
        ? { field: "reason", message: `Tối đa ${MAX_REASON_LENGTH} ký tự` }
        : null;
}
