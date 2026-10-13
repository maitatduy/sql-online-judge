import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AppProviders } from "./providers";

const mockWorker = vi.hoisted(() => {
    let markStarted: () => void = () => undefined;
    return {
        start: vi.fn(
            () =>
                new Promise<void>((resolve) => {
                    markStarted = resolve;
                }),
        ),
        finishStarting: () => markStarted(),
    };
});

vi.mock("@/mocks/browser", () => ({
    worker: { start: mockWorker.start },
}));

describe("AppProviders", () => {
    afterEach(() => {
        vi.unstubAllEnvs();
        mockWorker.start.mockClear();
    });

    it("hiện nội dung ngay khi không bật API giả", () => {
        render(
            <AppProviders>
                <p>Nội dung</p>
            </AppProviders>,
        );

        expect(screen.getByText("Nội dung")).toBeInTheDocument();
        expect(mockWorker.start).not.toHaveBeenCalled();
    });

    it("chờ API giả sẵn sàng rồi mới hiện nội dung, để request đầu tiên không lọt ra mạng thật", async () => {
        vi.stubEnv("NEXT_PUBLIC_API_MOCKING", "enabled");
        render(
            <AppProviders>
                <p>Nội dung</p>
            </AppProviders>,
        );

        await vi.waitFor(() => expect(mockWorker.start).toHaveBeenCalledTimes(1));
        expect(screen.queryByText("Nội dung")).not.toBeInTheDocument();

        mockWorker.finishStarting();

        expect(await screen.findByText("Nội dung")).toBeInTheDocument();
    });
});
