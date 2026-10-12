import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Logo } from "./Logo";

describe("Logo", () => {
    it("hai logo trên cùng một trang không trùng id", () => {
        const { container } = render(
            <>
                <Logo />
                <Logo />
            </>,
        );

        const maskIds = Array.from(container.querySelectorAll("mask")).map((mask) => mask.id);

        expect(maskIds).toHaveLength(2);
        expect(new Set(maskIds).size).toBe(2);
    });

    it("phần hình trụ tham chiếu đúng mask của chính logo đó", () => {
        const { container } = render(<Logo />);

        const maskId = container.querySelector("mask")?.id;

        expect(container.querySelector("g[mask]")).toHaveAttribute("mask", `url(#${maskId})`);
    });
});
