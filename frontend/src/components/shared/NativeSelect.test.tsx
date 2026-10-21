import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { NativeSelect } from "./NativeSelect";

describe("NativeSelect", () => {
    it("là select gốc của trình duyệt, nhận nhãn và giá trị", () => {
        render(
            <label>
                Dialect
                <NativeSelect defaultValue="POSTGRESQL">
                    <option value="POSTGRESQL">PostgreSQL</option>
                </NativeSelect>
            </label>,
        );

        expect(screen.getByRole("combobox", { name: "Dialect" })).toHaveValue("POSTGRESQL");
    });
});
