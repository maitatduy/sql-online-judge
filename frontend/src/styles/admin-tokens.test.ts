import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { describe, expect, it } from "vitest";

const MINIMUM_TEXT_CONTRAST = 4.5;
const WHITE = "#ffffff";
const LIGHT_SURFACE_LUMINANCE = 0.85;
const DARK_SURFACE_LUMINANCE = 0.05;

const SIDEBAR_TOKENS = [
    "sidebar",
    "sidebar-text",
    "sidebar-strong",
    "sidebar-muted",
    "sidebar-hover",
    "sidebar-active",
    "sidebar-line",
];
const STATUS_TOKENS = ["status-ok", "status-busy", "status-down"];
const METER_TOKENS = ["meter-normal", "meter-high", "meter-critical"];
const COUNT_TOKENS = [
    "count-review",
    "count-report",
    "count-ai-flag",
    "count-support",
    "count-live",
];
const ADMIN_TOKENS = [...SIDEBAR_TOKENS, ...STATUS_TOKENS, ...METER_TOKENS, ...COUNT_TOKENS];

const stylesheet = readFileSync(resolve(process.cwd(), "src/styles/globals.css"), "utf8");

function blockAfter(selector: string): string {
    const start = stylesheet.indexOf(`${selector} {`);
    return stylesheet.slice(start, stylesheet.indexOf("}", start));
}

function declaredValue(block: string, property: string): string | undefined {
    return new RegExp(`--${property}:\\s*([^;]+);`).exec(block)?.[1];
}

function channelLuminance(channel: number): number {
    const ratio = channel / 255;
    return ratio <= 0.03928 ? ratio / 12.92 : ((ratio + 0.055) / 1.055) ** 2.4;
}

function luminance(hexColor: string): number {
    const [red, green, blue] = [1, 3, 5].map((offset) =>
        channelLuminance(parseInt(hexColor.slice(offset, offset + 2), 16)),
    );
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

function contrast(foreground: string, background: string): number {
    const [lighter, darker] = [luminance(foreground), luminance(background)].sort(
        (first, second) => second - first,
    );
    return (lighter + 0.05) / (darker + 0.05);
}

const lightBlock = blockAfter(".light");
const darkBlock = blockAfter(".dark");
const themeBlock = blockAfter("@theme inline");

function lightValue(token: string): string {
    return declaredValue(lightBlock, token) ?? "";
}

function darkValue(token: string): string {
    return declaredValue(darkBlock, token) ?? "";
}

describe("token màu admin", () => {
    it.each(ADMIN_TOKENS)("%s được khai báo ở nền sáng, nền tối và @theme", (token) => {
        expect(lightValue(token)).toMatch(/^#[0-9a-f]{6}$/);
        expect(darkValue(token)).toMatch(/^#[0-9a-f]{6}$/);
        expect(declaredValue(themeBlock, `color-${token}`)).toBe(`var(--${token})`);
    });

    it.each(["sidebar-text", "sidebar-strong", "sidebar-muted"])(
        "%s đạt tương phản trên nền sidebar ở cả hai nền",
        (token) => {
            expect(contrast(lightValue(token), lightValue("sidebar"))).toBeGreaterThanOrEqual(
                MINIMUM_TEXT_CONTRAST,
            );
            expect(contrast(darkValue(token), darkValue("sidebar"))).toBeGreaterThanOrEqual(
                MINIMUM_TEXT_CONTRAST,
            );
        },
    );

    it.each([
        ["nền sáng", lightValue],
        ["nền tối", darkValue],
    ] as const)("chữ mục đang chọn và đang rê chuột đạt tương phản ở %s", (_theme, valueOf) => {
        expect(
            contrast(valueOf("sidebar-strong"), valueOf("sidebar-active")),
        ).toBeGreaterThanOrEqual(MINIMUM_TEXT_CONTRAST);
        expect(
            contrast(valueOf("sidebar-strong"), valueOf("sidebar-hover")),
        ).toBeGreaterThanOrEqual(MINIMUM_TEXT_CONTRAST);
    });

    it("sidebar sáng ở nền sáng và tối ở nền tối", () => {
        expect(luminance(lightValue("sidebar"))).toBeGreaterThan(LIGHT_SURFACE_LUMINANCE);
        expect(luminance(darkValue("sidebar"))).toBeLessThan(DARK_SURFACE_LUMINANCE);
    });

    it.each(STATUS_TOKENS)("%s đạt tương phản trên nền thẻ ở cả hai nền", (token) => {
        expect(contrast(lightValue(token), lightValue("bg"))).toBeGreaterThanOrEqual(
            MINIMUM_TEXT_CONTRAST,
        );
        expect(contrast(darkValue(token), darkValue("bg"))).toBeGreaterThanOrEqual(
            MINIMUM_TEXT_CONTRAST,
        );
    });

    it.each(COUNT_TOKENS)("chữ trắng trên %s đạt tương phản ở cả hai nền", (token) => {
        expect(contrast(WHITE, lightValue(token))).toBeGreaterThanOrEqual(MINIMUM_TEXT_CONTRAST);
        expect(contrast(WHITE, darkValue(token))).toBeGreaterThanOrEqual(MINIMUM_TEXT_CONTRAST);
    });
});
