"use client";

import { ErrorFallback } from "@/components/shared/ErrorFallback";

interface ErrorPageProps {
    error: Error & { digest?: string };
    reset: () => void;
}

export default function ErrorPage({ reset }: ErrorPageProps) {
    return <ErrorFallback onRetry={reset} className="min-h-screen" />;
}
