import { ApiRequestError } from "@/lib/api-client";

const UNEXPECTED_ERROR_MESSAGE = "Đã có lỗi xảy ra, vui lòng thử lại sau";
const INVALID_CREDENTIALS_MESSAGE = "Email hoặc mật khẩu không đúng";

export interface FieldError<TField extends string> {
    field: TField;
    message: string;
}

const CONFLICT_FIELD_ERRORS: Record<string, FieldError<string> | undefined> = {
    USERNAME_TAKEN: { field: "username", message: "Tên người dùng này đã có người dùng" },
    EMAIL_TAKEN: { field: "email", message: "Email này đã được đăng ký" },
};

export function getFieldErrors<TField extends string>(
    error: unknown,
    fields: readonly TField[],
): FieldError<TField>[] {
    if (!(error instanceof ApiRequestError)) {
        return [];
    }
    const conflictError = CONFLICT_FIELD_ERRORS[error.code];
    const candidates = conflictError ? [...error.errors, conflictError] : error.errors;
    return candidates.filter((fieldError): fieldError is FieldError<TField> =>
        fields.includes(fieldError.field as TField),
    );
}

function getRateLimitMessage(retryAfterSeconds: number | undefined): string {
    const waitTime = retryAfterSeconds ? `${retryAfterSeconds} giây` : "ít phút";
    return `Bạn thử quá nhiều lần. Vui lòng thử lại sau ${waitTime}.`;
}

export function getFormErrorMessage(error: unknown): string {
    if (!(error instanceof ApiRequestError)) {
        return UNEXPECTED_ERROR_MESSAGE;
    }
    if (error.status === 401) {
        return INVALID_CREDENTIALS_MESSAGE;
    }
    if (error.status === 429) {
        return getRateLimitMessage(error.retryAfterSeconds);
    }
    return error.message;
}
