import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Skeleton } from "./Skeleton";

describe("Skeleton", () => {
    it("là khối trang trí, ẩn với trình đọc màn hình và nhận kích thước từ ngoài", () => {
        const { container } = render(<Skeleton className="h-3 w-10" />);

        expect(container.firstChild).toHaveAttribute("aria-hidden", "true");
        expect(container.firstChild).toHaveClass("h-3", "w-10");
    });
});
