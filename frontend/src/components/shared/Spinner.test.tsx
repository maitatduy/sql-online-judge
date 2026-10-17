import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Spinner } from "./Spinner";

describe("Spinner", () => {
    it("là vòng tròn có vòng nền mờ và cung xoay, ẩn khỏi trình đọc màn hình", () => {
        render(<Spinner className="size-12" />);

        const spinner = screen.getByTestId("spinner");
        expect(spinner).toHaveAttribute("aria-hidden", "true");
        expect(spinner).toHaveClass("size-12", "motion-safe:animate-spin");
        expect(spinner.querySelectorAll("circle")).toHaveLength(2);
    });
});
