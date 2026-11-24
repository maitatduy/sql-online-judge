import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MINH_ANH_PROFILE, NEWBIE_PROFILE } from "@/mocks/profile-fixtures";

import { ProfileIdentityCard } from "./ProfileIdentityCard";

describe("ProfileIdentityCard", () => {
    it("hiện tên, username, rating, tiểu sử, trường và nơi ở", () => {
        render(
            <ProfileIdentityCard
                profile={MINH_ANH_PROFILE.profile}
                rating={MINH_ANH_PROFILE.rating}
            />,
        );

        expect(
            screen.getByRole("heading", { level: 1, name: "Nguyễn Minh Anh" }),
        ).toBeInTheDocument();
        expect(screen.getByText("@minh.anh")).toBeInTheDocument();
        expect(screen.getByText("Rating 1.487")).toBeInTheDocument();
        expect(
            screen.getByText(
                "Sinh viên năm 3 Khoa học dữ liệu. Đang luyện SQL để đi thực tập Data Analyst.",
            ),
        ).toBeInTheDocument();
        expect(screen.getByText("ĐH Bách khoa Hà Nội")).toBeInTheDocument();
        expect(screen.getByText("Hà Nội, Việt Nam")).toBeInTheDocument();
    });

    it("link ngoài mở tab mới và không truyền uy tín cho trang đích", () => {
        render(
            <ProfileIdentityCard
                profile={MINH_ANH_PROFILE.profile}
                rating={MINH_ANH_PROFILE.rating}
            />,
        );

        const link = screen.getByRole("link", { name: "github.com/minhanh-data" });

        expect(link).toHaveAttribute("href", "https://github.com/minhanh-data");
        expect(link).toHaveAttribute("target", "_blank");
        expect(link).toHaveAttribute("rel", "noopener noreferrer nofollow ugc");
    });

    it("không tạo link khi địa chỉ không phải http(s)", () => {
        render(
            <ProfileIdentityCard
                profile={{ ...MINH_ANH_PROFILE.profile, websiteUrl: "javascript:alert(1)" }}
                rating={undefined}
            />,
        );

        expect(screen.queryByRole("link")).not.toBeInTheDocument();
    });

    it("bỏ các dòng không có dữ liệu và không hiện rating khi chưa thi", () => {
        render(
            <ProfileIdentityCard profile={NEWBIE_PROFILE.profile} rating={NEWBIE_PROFILE.rating} />,
        );

        expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Trần Thu Hà");
        expect(screen.queryByText(/^Rating/)).not.toBeInTheDocument();
        expect(screen.queryByRole("list", { name: "Thông tin thêm" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link")).not.toBeInTheDocument();
    });
});
