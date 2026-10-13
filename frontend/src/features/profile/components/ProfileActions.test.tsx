import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ProfileActions } from "./ProfileActions";

describe("ProfileActions", () => {
    it("chủ hồ sơ thấy nút sửa hồ sơ dẫn tới cài đặt", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<ProfileActions username="minh.anh" />);

        expect(await screen.findByRole("link", { name: "Sửa hồ sơ" })).toHaveAttribute(
            "href",
            "/settings",
        );
    });

    it("người đăng nhập khác không thấy nút sửa hồ sơ", async () => {
        let requestCount = 0;
        server.use(
            http.get("*/api/auth/me", () => {
                requestCount += 1;
                return HttpResponse.json(DEMO_SESSION_USER);
            }),
        );
        renderWithProviders(<ProfileActions username="newbie" />);

        await waitFor(() => expect(requestCount).toBe(1));

        expect(screen.queryByRole("link", { name: "Sửa hồ sơ" })).not.toBeInTheDocument();
    });

    it("khách vẫn chia sẻ được hồ sơ nhưng không sửa được", () => {
        renderWithProviders(<ProfileActions username="minh.anh" />);

        expect(screen.getByRole("button", { name: "Chia sẻ hồ sơ" })).toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Sửa hồ sơ" })).not.toBeInTheDocument();
    });
});
