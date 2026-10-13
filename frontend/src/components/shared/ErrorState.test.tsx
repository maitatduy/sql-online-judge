import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ErrorState } from "./ErrorState";

describe("ErrorState", () => {
    it("báo lỗi kèm mã lỗi để người dùng gửi khi cần hỗ trợ", () => {
        render(
            <ErrorState
                title="Không tải được hồ sơ"
                message="Đã có lỗi xảy ra, vui lòng thử lại sau"
                traceId="4bf92f3577b34da6"
                onRetry={vi.fn()}
            />,
        );

        const alert = screen.getByRole("alert");

        expect(alert).toHaveTextContent("Không tải được hồ sơ");
        expect(alert).toHaveTextContent("Đã có lỗi xảy ra, vui lòng thử lại sau");
        expect(alert).toHaveTextContent("Mã lỗi: 4bf92f3577b34da6");
    });

    it("không hiện dòng mã lỗi khi không có traceId", () => {
        render(<ErrorState title="Không tải được hồ sơ" onRetry={vi.fn()} />);

        expect(screen.queryByText(/Mã lỗi/)).not.toBeInTheDocument();
    });

    it("nút thử lại gọi onRetry", () => {
        const onRetry = vi.fn();
        render(<ErrorState title="Không tải được hồ sơ" onRetry={onRetry} />);

        fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));

        expect(onRetry).toHaveBeenCalledTimes(1);
    });
});
