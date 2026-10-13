import "@testing-library/jest-dom/vitest";

import { cleanup, configure } from "@testing-library/react";
import { afterAll, afterEach, beforeAll } from "vitest";

import { server } from "@/mocks/server";

const ASYNC_QUERY_TIMEOUT_MS = 5000;

configure({ asyncUtilTimeout: ASYNC_QUERY_TIMEOUT_MS });

class ResizeObserverStub implements ResizeObserver {
    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
}

globalThis.ResizeObserver ??= ResizeObserverStub;

beforeAll(() => {
    server.listen({ onUnhandledRequest: "error" });
});

afterEach(() => {
    cleanup();
    server.resetHandlers();
    sessionStorage.clear();
});

afterAll(() => {
    server.close();
});
