"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState } from "react";

import { createQueryClient } from "@/lib/query-client";

async function startMockWorker(): Promise<void> {
    if (
        process.env.NODE_ENV !== "production" &&
        process.env.NEXT_PUBLIC_API_MOCKING === "enabled"
    ) {
        const { worker } = await import("@/mocks/browser");
        await worker.start({ onUnhandledRequest: "bypass", quiet: true });
    }
}

interface AppProvidersProps {
    children: React.ReactNode;
}

export function AppProviders({ children }: AppProvidersProps) {
    const [queryClient] = useState(createQueryClient);
    const [mockingError, setMockingError] = useState<Error | null>(null);

    useEffect(() => {
        startMockWorker().catch((error: unknown) => {
            setMockingError(error instanceof Error ? error : new Error(String(error)));
        });
    }, []);

    if (mockingError) {
        throw mockingError;
    }

    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
