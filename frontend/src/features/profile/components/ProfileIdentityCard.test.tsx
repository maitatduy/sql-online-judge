import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MINH_ANH_PROFILE, NEWBIE_PROFILE } from "@/mocks/profile-fixtures";

import { ProfileIdentityCard } from "./ProfileIdentityCard";

describe("ProfileIdentityCard", () => {
    it("hiện tên, username, hạng kèm rating và tiểu sử", () => {
        render(<ProfileIdentityCard profile={MINH_ANH_PROFILE} />);

        expect(
            screen.getByRole("heading", { level: 1, name: "Nguyễn Minh Anh" }),
        ).toBeInTheDocument();
        expect(screen.getByText("@minh.anh")).toBeInTheDocument();
        expect(screen.getByText("Chuyên viên · 1.487")).toBeInTheDocument();
        expect(
            screen.getByText(
                "Sinh viên năm 3 Khoa học dữ liệu. Đang luyện SQL để đi thực tập Data Analyst.",
            ),
        ).toBeInTheDocument();
    });

    it("hiện trường, nơi ở và số người theo dõi", () => {
        render(<ProfileIdentityCard profile={MINH_ANH_PROFILE} />);

        expect(screen.getByText("ĐH Bách khoa Hà Nội")).toBeInTheDocument();
        expect(screen.getByText("Hà Nội, Việt Nam")).toBeInTheDocument();
        expect(screen.getByText("128")).toBeInTheDocument();
        expect(screen.getByText("người theo dõi")).toBeInTheDocument();
        expect(screen.getByText("41")).toBeInTheDocument();
        expect(screen.getByText("đang theo dõi")).toBeInTheDocument();
    });

    it("link ngoài mở tab mới và không truyền uy tín cho trang đích", () => {
        render(<ProfileIdentityCard profile={MINH_ANH_PROFILE} />);

        const link = screen.getByRole("link", { name: "github.com/minhanh-data" });

        expect(link).toHaveAttribute("href", "https://github.com/minhanh-data");
        expect(link).toHaveAttribute("target", "_blank");
        expect(link).toHaveAttribute("rel", "noopener noreferrer nofollow ugc");
    });

    it("không tạo link khi địa chỉ không phải http(s)", () => {
        render(
            <ProfileIdentityCard
                profile={{ ...MINH_ANH_PROFILE, websiteUrl: "javascript:alert(1)" }}
            />,
        );

        expect(screen.queryByRole("link")).not.toBeInTheDocument();
    });

    it("bỏ các dòng không có dữ liệu", () => {
        render(<ProfileIdentityCard profile={NEWBIE_PROFILE} />);

        expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
            NEWBIE_PROFILE.displayName,
        );
        expect(screen.queryByRole("list", { name: "Thông tin thêm" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link")).not.toBeInTheDocument();
    });
});
