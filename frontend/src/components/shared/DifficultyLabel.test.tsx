import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { DifficultyLabel } from "./DifficultyLabel";

describe("DifficultyLabel", () => {
    it.each([
        ["EASY", "Dễ", "text-difficulty-easy"],
        ["MEDIUM", "Trung bình", "text-difficulty-medium"],
        ["HARD", "Khó", "text-difficulty-hard"],
    ] as const)(
        "hiện độ khó %s bằng chữ %s và màu token riêng",
        (difficulty, label, colorClass) => {
            render(<DifficultyLabel difficulty={difficulty} />);

            expect(screen.getByText(label)).toHaveClass(colorClass);
        },
    );
});
