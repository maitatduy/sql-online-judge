import { describe, expect, it } from "vitest";

import { avatarFileError, MAX_AVATAR_BYTES } from "./avatar-file";

function fileOf(type: string, size: number) {
    return new File([new Uint8Array(size)], "avatar", { type });
}

describe("avatarFileError", () => {
    it.each(["image/png", "image/jpeg", "image/webp"])("nhận ảnh %s", (type) => {
        expect(avatarFileError(fileOf(type, 10))).toBeNull();
    });

    it.each(["image/gif", "image/svg+xml", "application/pdf", ""])(
        "từ chối tệp loại '%s'",
        (type) => {
            expect(avatarFileError(fileOf(type, 10))).toBe("Ảnh phải là PNG, JPG hoặc WEBP");
        },
    );

    it("nhận ảnh đúng bằng giới hạn dung lượng", () => {
        expect(avatarFileError(fileOf("image/png", MAX_AVATAR_BYTES))).toBeNull();
    });

    it("từ chối ảnh lớn hơn 2 MB", () => {
        expect(avatarFileError(fileOf("image/png", MAX_AVATAR_BYTES + 1))).toBe("Ảnh tối đa 2 MB");
    });
});
