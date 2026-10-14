import { ApiRequestError } from "@/lib/api-client";

export function fieldErrorOf(error: unknown, field: string): string | undefined {
    return error instanceof ApiRequestError
        ? error.errors.find((fieldError) => fieldError.field === field)?.message
        : undefined;
}
