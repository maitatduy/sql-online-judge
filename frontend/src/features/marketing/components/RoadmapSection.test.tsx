import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import type { RoadmapStage } from "../types";

import { RoadmapSection } from "./RoadmapSection";

const stages: RoadmapStage[] = [
    { title: "Cơ bản", topics: "SELECT, WHERE, ORDER BY", level: "FOUNDATION" },
    { title: "JOIN", topics: "INNER, LEFT, SELF JOIN", level: "FOUNDATION" },
    { title: "Tối ưu", topics: "Index, EXPLAIN", level: "ADVANCED" },
];

describe("RoadmapSection", () => {
    it("có tiêu đề khối", () => {
        render(<RoadmapSection stages={stages} />);

        expect(
            screen.getByRole("heading", { level: 2, name: "Sáu chặng, đi theo tốc độ của bạn" }),
        ).toBeInTheDocument();
    });

    it("liệt kê các chặng theo thứ tự, có đánh số", () => {
        render(<RoadmapSection stages={stages} />);

        const items = within(screen.getByRole("list")).getAllByRole("listitem");

        expect(items).toHaveLength(3);
        expect(within(items[0]).getByText("01")).toBeInTheDocument();
        expect(within(items[0]).getByText("Cơ bản")).toBeInTheDocument();
        expect(within(items[2]).getByText("03")).toBeInTheDocument();
        expect(within(items[2]).getByText("Index, EXPLAIN")).toBeInTheDocument();
    });
});
