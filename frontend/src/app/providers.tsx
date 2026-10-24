"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState } from "react";

import { createQueryClient } from "@/lib/query-client";

function isApiMockingEnabled(): boolean {
    return (
        process.env.NODE_ENV !== "production" && process.env.NEXT_PUBLIC_API_MOCKING === "enabled"
    );
}

let mockWorkerStart: Promise<void> | null = null;

async function launchMockWorker(): Promise<void> {
    const { worker } = await import("@/mocks/browser");
    await worker.start({ onUnhandledRequest: "bypass", quiet: true });
}

function startMockWorker(): Promise<void> {
    mockWorkerStart ??= launchMockWorker().catch((error: unknown) => {
        mockWorkerStart = null;
        throw error;
    });
    return mockWorkerStart;
}

interface AppProvidersProps {
    children: React.ReactNode;
}

export function AppProviders({ children }: AppProvidersProps) {
    const [queryClient] = useState(createQueryClient);
    const [isApiReady, setIsApiReady] = useState(() => !isApiMockingEnabled());
    const [mockingError, setMockingError] = useState<Error | null>(null);

    useEffect(() => {
        if (isApiReady) {
            return;
        }
        startMockWorker()
            .then(() => setIsApiReady(true))
            .catch((error: unknown) => {
                setMockingError(error instanceof Error ? error : new Error(String(error)));
            });
    }, [isApiReady]);

    if (mockingError) {
        throw mockingError;
    }

    return (
        <QueryClientProvider client={queryClient}>
            {isApiReady ? children : null}
        </QueryClientProvider>
    );
}
