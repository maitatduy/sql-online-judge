import { ApiRequestError } from "@/lib/api-client";

export function serverFieldErrorOf(error: unknown, field: string): string | undefined {
    if (!(error instanceof ApiRequestError)) {
        return undefined;
    }
    if (field === "slug" && error.code === "SLUG_TAKEN") {
        return error.message;
    }
    return error.errors.find((fieldError) => fieldError.field === field)?.message;
}
