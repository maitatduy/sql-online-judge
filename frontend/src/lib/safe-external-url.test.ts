import { describe, expect, it } from "vitest";

import { displayUrl, safeExternalUrl } from "./safe-external-url";

describe("safeExternalUrl", () => {
    it.each(["https://github.com/minhanh-data", "http://example.com/a?b=1"])(
        "giữ nguyên link %s",
        (url) => {
            expect(safeExternalUrl(url)).toBe(url);
        },
    );

    it.each([
        "javascript:alert(1)",
        "data:text/html,<script>alert(1)</script>",
        "ftp://example.com",
        "//example.com",
        "github.com/minhanh-data",
        "",
        undefined,
    ])("từ chối %j", (url) => {
        expect(safeExternalUrl(url)).toBeNull();
    });
});

describe("displayUrl", () => {
    it.each([
        ["https://github.com/minhanh-data", "github.com/minhanh-data"],
        ["https://www.example.com/", "example.com"],
        ["http://example.com/a/", "example.com/a"],
    ])("rút gọn %s thành %s", (url, label) => {
        expect(displayUrl(url)).toBe(label);
    });
});
