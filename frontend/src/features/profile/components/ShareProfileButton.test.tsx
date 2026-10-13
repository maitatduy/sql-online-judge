import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ShareProfileButton } from "./ShareProfileButton";

function stubClipboard(writeText: (text: string) => Promise<void>) {
    Object.defineProperty(navigator, "clipboard", {
        value: { writeText },
        configurable: true,
    });
}

describe("ShareProfileButton", () => {
    afterEach(() => {
        Reflect.deleteProperty(navigator, "clipboard");
    });

    it("sao chép đường dẫn hồ sơ và báo đã sao chép", async () => {
        const writeText = vi.fn().mockResolvedValue(undefined);
        stubClipboard(writeText);
        render(<ShareProfileButton username="minh.anh" />);

        fireEvent.click(screen.getByRole("button", { name: "Chia sẻ hồ sơ" }));

        expect(await screen.findByText("Đã sao chép liên kết hồ sơ")).toBeInTheDocument();
        expect(writeText).toHaveBeenCalledWith(`${window.location.origin}/u/minh.anh`);
    });

    it("báo rõ khi trình duyệt không cho sao chép", async () => {
        stubClipboard(vi.fn().mockRejectedValue(new Error("denied")));
        render(<ShareProfileButton username="minh.anh" />);

        fireEvent.click(screen.getByRole("button", { name: "Chia sẻ hồ sơ" }));

        expect(await screen.findByText("Không sao chép được liên kết")).toBeInTheDocument();
    });

    it("thông báo nằm trong vùng trạng thái để trình đọc màn hình đọc lên", () => {
        stubClipboard(vi.fn().mockResolvedValue(undefined));
        render(<ShareProfileButton username="minh.anh" />);

        expect(screen.getByRole("status")).toBeEmptyDOMElement();
    });
});
