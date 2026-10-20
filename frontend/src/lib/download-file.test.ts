import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { stubObjectUrls } from "@/test/downloads";

import { downloadFile } from "./download-file";

describe("downloadFile", () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    it("tải blob bằng thẻ a tạm có thuộc tính download rồi gỡ thẻ ngay", () => {
        const objectUrls = stubObjectUrls();
        const clicked: { href?: string; download?: string; isAttached?: boolean } = {};
        vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (
            this: HTMLAnchorElement,
        ) {
            clicked.href = this.href;
            clicked.download = this.download;
            clicked.isAttached = document.body.contains(this);
        });
        const blob = new Blob(["a,b\n"], { type: "text/csv" });

        downloadFile(blob, "users-20261020-0930.csv");

        expect(objectUrls.created).toEqual([blob]);
        expect(clicked).toEqual({
            href: objectUrls.url,
            download: "users-20261020-0930.csv",
            isAttached: true,
        });
        expect(document.querySelector("a[download]")).toBeNull();
    });

    it("giữ object URL khoảng 1 giây để trình duyệt kịp bắt đầu tải rồi mới thu hồi", () => {
        const objectUrls = stubObjectUrls();
        vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined);

        downloadFile(new Blob(["a,b\n"], { type: "text/csv" }), "users.csv");

        vi.advanceTimersByTime(999);
        expect(objectUrls.revoked).toEqual([]);
        vi.advanceTimersByTime(1);
        expect(objectUrls.revoked).toEqual([objectUrls.url]);
    });
});
