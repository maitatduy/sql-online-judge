"use client";

import { ErrorFallback } from "@/components/shared/ErrorFallback";

interface AppErrorPageProps {
    error: Error & { digest?: string };
    reset: () => void;
}

export default function AppErrorPage({ reset }: AppErrorPageProps) {
    return <ErrorFallback onRetry={reset} className="py-24" />;
}
