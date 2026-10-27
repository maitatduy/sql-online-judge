import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ApiRequestError, isUnauthenticatedError } from "@/lib/api-client";

const RATE_LIMITED_STATUS = 429;

interface ActionErrorProps {
    error: Error;
    loginMessage: string;
    loginHref: string;
    rateLimitedMessage: string;
    onRetry: () => void;
}

export function ActionError({
    error,
    loginMessage,
    loginHref,
    rateLimitedMessage,
    onRetry,
}: ActionErrorProps) {
    if (isUnauthenticatedError(error)) {
        return (
            <p className="text-[13px]">
                {loginMessage}{" "}
                <Link href={loginHref} className="font-semibold text-brand">
                    Đăng nhập
                </Link>
            </p>
        );
    }
    if (error instanceof ApiRequestError && error.status === RATE_LIMITED_STATUS) {
        return (
            <p role="alert" className="text-[13px]">
                {error.retryAfterSeconds
                    ? `${rateLimitedMessage} Thử lại sau ${error.retryAfterSeconds} giây.`
                    : `${rateLimitedMessage} Thử lại sau ít phút.`}
            </p>
        );
    }
    return (
        <div role="alert" className="flex items-center gap-3 text-[13px]">
            <span>{error.message}</span>
            <Button type="button" size="sm" variant="outline" onClick={onRetry}>
                Thử lại
            </Button>
        </div>
    );
}
