import { HttpResponse } from "msw";

interface ApiErrorExtras {
    headers?: HeadersInit;
    traceId?: string;
}

export function apiError(
    status: number,
    code: string,
    message: string,
    { headers, traceId }: ApiErrorExtras = {},
) {
    return HttpResponse.json({ status, code, message, traceId }, { status, headers });
}
