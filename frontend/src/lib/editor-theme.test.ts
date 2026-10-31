import { describe, expect, it } from "vitest";

import { editorThemeFrom } from "./editor-theme";

const DARK_TOKENS: Record<string, string> = {
    "--bg": "#0b0f17",
    "--surface-2": "#182133",
    "--line": "#1f2937",
    "--text": "#e5e7eb",
    "--text-muted": "#94a3b8",
    "--brand": "#3b82f6",
    "--syntax-keyword": "#c4a5ff",
    "--syntax-function": "#7db4ff",
    "--syntax-string": "#fdba74",
    "--syntax-number": "#5eead4",
    "--syntax-comment": "#6b7a90",
};

const readFrom = (tokens: Record<string, string>) => (name: string) => tokens[name] ?? "";

describe("editorThemeFrom", () => {
    it("dựng theme tối của Monaco từ token màu của design system", () => {
        const theme = editorThemeFrom(readFrom(DARK_TOKENS), "vs-dark");

        expect(theme.base).toBe("vs-dark");
        expect(theme.inherit).toBe(true);
        expect(theme.colors).toMatchObject({
            "editor.background": "#0b0f17",
            "editor.foreground": "#e5e7eb",
            "editorLineNumber.foreground": "#94a3b8",
            "editorLineNumber.activeForeground": "#e5e7eb",
            "editorCursor.foreground": "#3b82f6",
            "editor.lineHighlightBackground": "#182133",
            "editorWidget.border": "#1f2937",
        });
    });

    it("tô cú pháp SQL bằng các token syntax, bỏ dấu # theo định dạng của Monaco", () => {
        const theme = editorThemeFrom(readFrom(DARK_TOKENS), "vs-dark");

        expect(theme.rules).toEqual(
            expect.arrayContaining([
                { token: "keyword", foreground: "c4a5ff", fontStyle: "bold" },
                { token: "predefined", foreground: "7db4ff" },
                { token: "string", foreground: "fdba74" },
                { token: "number", foreground: "5eead4" },
                { token: "comment", foreground: "6b7a90", fontStyle: "italic" },
            ]),
        );
    });

    it("chuẩn hoá giá trị token có khoảng trắng và chữ hoa", () => {
        const theme = editorThemeFrom(
            readFrom({ ...DARK_TOKENS, "--bg": "  #0B0F17 " }),
            "vs-dark",
        );

        expect(theme.colors["editor.background"]).toBe("#0b0f17");
    });

    it("bỏ qua token thiếu hoặc không phải mã hex 6 chữ số thay vì đưa màu hỏng cho Monaco", () => {
        const theme = editorThemeFrom(
            readFrom({ ...DARK_TOKENS, "--syntax-string": "", "--bg": "oklch(0.2 0 0)" }),
            "vs-dark",
        );

        expect(theme.colors).not.toHaveProperty("editor.background");
        expect(theme.rules.map((rule) => rule.token)).not.toContain("string");
    });

    it("dựng trên theme sáng của Monaco khi editor nằm trên nền sáng", () => {
        expect(editorThemeFrom(readFrom(DARK_TOKENS), "vs").base).toBe("vs");
    });
});
