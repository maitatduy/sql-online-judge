import { clearAccessToken, getAccessToken, setAccessToken } from "./auth-session";

const NETWORK_ERROR_MESSAGE = "Không kết nối được máy chủ, vui lòng thử lại";
const UNEXPECTED_ERROR_MESSAGE = "Đã có lỗi xảy ra, vui lòng thử lại sau";
const UNAUTHENTICATED_STATUS = 401;
const AUTH_PATH_PREFIX = "/api/auth/";
const CURRENT_USER_PATH = "/api/auth/me";
const REFRESH_PATH = "/api/auth/refresh";
const COOKIE_SESSION_PATHS = new Set([REFRESH_PATH, "/api/auth/logout"]);

interface RefreshedSession {
    accessToken: string;
}

let refreshInFlight: Promise<boolean> | null = null;

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

function isJsonBody(body: unknown): boolean {
    return body !== undefined && !(body instanceof FormData);
}

function usesCookieSession(path: string): boolean {
    return COOKIE_SESSION_PATHS.has(path);
}

function canRefreshAfter(path: string): boolean {
    return !path.startsWith(AUTH_PATH_PREFIX) || path === CURRENT_USER_PATH;
}

function buildHeaders(path: string, body: unknown): HeadersInit {
    const headers: Record<string, string> = {};
    if (isJsonBody(body)) {
        headers["Content-Type"] = "application/json";
    }
    const accessToken = getAccessToken();
    if (accessToken && !usesCookieSession(path)) {
        headers.Authorization = `Bearer ${accessToken}`;
    }
    return headers;
}

async function refreshSession(): Promise<boolean> {
    try {
        const response = await fetch(buildUrl(REFRESH_PATH), {
            method: "POST",
            credentials: "include",
        });
        if (!response.ok) {
            clearAccessToken();
            return false;
        }
        const session = (await response.json()) as RefreshedSession;
        setAccessToken(session.accessToken);
        return true;
    } catch {
        clearAccessToken();
        return false;
    }
}

function refreshOnce(): Promise<boolean> {
    refreshInFlight ??= refreshSession().finally(() => {
        refreshInFlight = null;
    });
    return refreshInFlight;
}

function sendRequest(
    method: string,
    path: string,
    body: unknown,
    options: RequestOptions,
): Promise<Response> {
    return send(buildUrl(path, options.params), {
        method,
        credentials: "include",
        signal: options.signal,
        headers: buildHeaders(path, body),
        body: isJsonBody(body) ? JSON.stringify(body) : (body as FormData | undefined),
    });
}

async function request<T>(
    method: string,
    path: string,
    body: unknown,
    options: RequestOptions,
): Promise<T> {
    let response = await sendRequest(method, path, body, options);
    if (
        response.status === UNAUTHENTICATED_STATUS &&
        canRefreshAfter(path) &&
        (await refreshOnce())
    ) {
        response = await sendRequest(method, path, body, options);
    }
    if (!response.ok) {
        throw await toRequestError(response);
    }
    const responseText = await response.text();
    return (responseText ? JSON.parse(responseText) : undefined) as T;
}

export const apiClient = {
    get: <T>(path: string, options: RequestOptions = {}) =>
        request<T>("GET", path, undefined, options),
    post: <T>(path: string, body?: unknown, options: RequestOptions = {}) =>
        request<T>("POST", path, body, options),
    patch: <T>(path: string, body: unknown, options: RequestOptions = {}) =>
        request<T>("PATCH", path, body, options),
    delete: <T>(path: string, body?: unknown, options: RequestOptions = {}) =>
        request<T>("DELETE", path, body, options),
    putForm: <T>(path: string, form: FormData, options: RequestOptions = {}) =>
        request<T>("PUT", path, form, options),
};
