const NETWORK_ERROR_MESSAGE = "Không kết nối được máy chủ, vui lòng thử lại";
const UNEXPECTED_ERROR_MESSAGE = "Đã có lỗi xảy ra, vui lòng thử lại sau";
const UNAUTHENTICATED_STATUS = 401;

export interface ApiFieldError {
    field: string;
    message: string;
}

export interface ApiErrorBody {
    status: number;
    code: string;
    message: string;
    errors?: ApiFieldError[];
    traceId?: string;
    timestamp?: string;
}

interface ApiRequestErrorDetails {
    status: number;
    code: string;
    message: string;
    errors?: ApiFieldError[];
    retryAfterSeconds?: number;
    traceId?: string;
}

export class ApiRequestError extends Error {
    readonly status: number;
    readonly code: string;
    readonly errors: ApiFieldError[];
    readonly retryAfterSeconds?: number;
    readonly traceId?: string;

    constructor({
        status,
        code,
        message,
        errors,
        retryAfterSeconds,
        traceId,
    }: ApiRequestErrorDetails) {
        super(message);
        this.name = "ApiRequestError";
        this.status = status;
        this.code = code;
        this.errors = errors ?? [];
        this.retryAfterSeconds = retryAfterSeconds;
        this.traceId = traceId;
    }
}

export function isUnauthenticatedError(error: unknown): boolean {
    return error instanceof ApiRequestError && error.status === UNAUTHENTICATED_STATUS;
}

type QueryParams = Record<string, string | number | boolean | undefined>;

interface RequestOptions {
    params?: QueryParams;
    signal?: AbortSignal;
}

function getBaseUrl(): string {
    const configuredUrl = process.env.NEXT_PUBLIC_API_URL;
    if (configuredUrl) {
        return configuredUrl;
    }
    return typeof window === "undefined" ? "" : window.location.origin;
}

function buildUrl(path: string, params?: QueryParams): string {
    const query = new URLSearchParams();
    Object.entries(params ?? {}).forEach(([key, value]) => {
        if (value !== undefined) {
            query.set(key, String(value));
        }
    });
    const queryString = query.toString();
    return `${getBaseUrl()}${path}${queryString ? `?${queryString}` : ""}`;
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
    return (
        typeof value === "object" &&
        value !== null &&
        typeof (value as ApiErrorBody).code === "string" &&
        typeof (value as ApiErrorBody).message === "string"
    );
}

function parseRetryAfter(response: Response): number | undefined {
    const seconds = Number(response.headers.get("Retry-After"));
    return Number.isInteger(seconds) && seconds > 0 ? seconds : undefined;
}

async function toRequestError(response: Response): Promise<ApiRequestError> {
    const body: unknown = await response.json().catch(() => null);
    const retryAfterSeconds = parseRetryAfter(response);
    if (isApiErrorBody(body)) {
        return new ApiRequestError({
            status: response.status,
            code: body.code,
            message: body.message,
            errors: body.errors,
            retryAfterSeconds,
            traceId: body.traceId,
        });
    }
    return new ApiRequestError({
        status: response.status,
        code: "UNEXPECTED_ERROR",
        message: UNEXPECTED_ERROR_MESSAGE,
        retryAfterSeconds,
    });
}

async function send(url: string, init: RequestInit): Promise<Response> {
    try {
        return await fetch(url, init);
    } catch {
        throw new ApiRequestError({
            status: 0,
            code: "NETWORK_ERROR",
            message: NETWORK_ERROR_MESSAGE,
        });
    }
}

async function request<T>(
    method: string,
    path: string,
    body: unknown,
    options: RequestOptions,
): Promise<T> {
    const response = await send(buildUrl(path, options.params), {
        method,
        credentials: "include",
        signal: options.signal,
        headers: body === undefined ? undefined : { "Content-Type": "application/json" },
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!response.ok) {
        throw await toRequestError(response);
    }
    if (response.status === 204) {
        return undefined as T;
    }
    return (await response.json()) as T;
}

export const apiClient = {
    get: <T>(path: string, options: RequestOptions = {}) =>
        request<T>("GET", path, undefined, options),
    post: <T>(path: string, body?: unknown, options: RequestOptions = {}) =>
        request<T>("POST", path, body, options),
};
