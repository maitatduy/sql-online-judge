import { render, screen } from "@testing-library/react";
import { Component } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mockWorker = vi.hoisted(() => {
    const pendingStarts: Array<() => void> = [];
    return {
        start: vi.fn(
            () =>
                new Promise<void>((resolve) => {
                    pendingStarts.push(resolve);
                }),
        ),
        finishStarting: () => pendingStarts.splice(0).forEach((resolve) => resolve()),
    };
});

class ErrorBoundary extends Component<{ children: React.ReactNode }, { hasError: boolean }> {
    state = { hasError: false };

    static getDerivedStateFromError() {
        return { hasError: true };
    }

    render() {
        return this.state.hasError ? <p>Lỗi khởi động</p> : this.props.children;
    }
}

async function loadAppProviders() {
    const { AppProviders } = await import("./providers");
    return AppProviders;
}

describe("AppProviders", () => {
    beforeEach(() => {
        vi.resetModules();
        vi.doMock("@/mocks/browser", () => ({ worker: { start: mockWorker.start } }));
    });

    afterEach(() => {
        vi.unstubAllEnvs();
        vi.restoreAllMocks();
        mockWorker.start.mockReset();
    });

    it("hiện nội dung ngay khi không bật API giả", async () => {
        const AppProviders = await loadAppProviders();
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
        const AppProviders = await loadAppProviders();
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

    it("chỉ khởi động API giả một lần khi chuyển sang khung trang khác", async () => {
        vi.stubEnv("NEXT_PUBLIC_API_MOCKING", "enabled");
        mockWorker.start.mockResolvedValue(undefined);
        const AppProviders = await loadAppProviders();
        render(
            <AppProviders>
                <p>Khung đăng nhập</p>
            </AppProviders>,
        );
        expect(await screen.findByText("Khung đăng nhập")).toBeInTheDocument();

        render(
            <AppProviders>
                <p>Khung ứng dụng</p>
            </AppProviders>,
        );

        expect(await screen.findByText("Khung ứng dụng")).toBeInTheDocument();
        expect(mockWorker.start).toHaveBeenCalledTimes(1);
    });

    it("thử khởi động lại API giả khi lần trước thất bại", async () => {
        vi.stubEnv("NEXT_PUBLIC_API_MOCKING", "enabled");
        mockWorker.start.mockRejectedValueOnce(new Error("Không đăng ký được service worker"));
        vi.spyOn(console, "error").mockImplementation(() => undefined);
        const AppProviders = await loadAppProviders();
        const firstMount = render(
            <ErrorBoundary>
                <AppProviders>
                    <p>Nội dung</p>
                </AppProviders>
            </ErrorBoundary>,
        );
        expect(await screen.findByText("Lỗi khởi động")).toBeInTheDocument();
        firstMount.unmount();

        render(
            <AppProviders>
                <p>Nội dung</p>
            </AppProviders>,
        );
        await vi.waitFor(() => expect(mockWorker.start).toHaveBeenCalledTimes(2));
        mockWorker.finishStarting();

        expect(await screen.findByText("Nội dung")).toBeInTheDocument();
    });
});
