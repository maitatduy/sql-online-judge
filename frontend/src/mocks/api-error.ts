import { HttpResponse } from "msw";

import type { ApiFieldError } from "@/lib/api-client";

interface ApiErrorExtras {
    headers?: HeadersInit;
    traceId?: string;
    errors?: ApiFieldError[];
}

export function apiError(
    status: number,
    code: string,
    message: string,
    { headers, traceId, errors }: ApiErrorExtras = {},
) {
    return HttpResponse.json({ status, code, message, errors, traceId }, { status, headers });
}
