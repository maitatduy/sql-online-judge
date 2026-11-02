import { describe, expect, it, vi } from "vitest";

const loaded = vi.hoisted(() => new Set<string>());

vi.mock("@monaco-editor/react", () => ({ loader: { config: vi.fn() } }));
vi.mock("monaco-editor/editor/editor.api", () => ({}));
vi.mock("monaco-editor/languages/definitions/pgsql/register", () => {
    loaded.add("pgsql");
    return {};
});
vi.mock("monaco-editor/editor/contrib/suggest/browser/suggestController", () => {
    loaded.add("suggest");
    return {};
});
vi.mock("monaco-editor/editor/contrib/bracketMatching/browser/bracketMatching", () => {
    loaded.add("bracketMatching");
    return {};
});
vi.mock("monaco-editor/editor/contrib/comment/browser/comment", () => {
    loaded.add("comment");
    return {};
});
vi.mock("monaco-editor/editor/contrib/find/browser/findController", () => {
    loaded.add("find");
    return {};
});
vi.mock("monaco-editor/editor/contrib/wordHighlighter/browser/wordHighlighter", () => {
    loaded.add("wordHighlighter");
    return {};
});
vi.mock("monaco-editor/editor/contrib/linesOperations/browser/linesOperations", () => {
    loaded.add("linesOperations");
    return {};
});
vi.mock("monaco-editor/editor/contrib/contextmenu/browser/contextmenu", () => {
    loaded.add("contextmenu");
    return {};
});
vi.mock("monaco-editor/editor/contrib/clipboard/browser/clipboard", () => {
    loaded.add("clipboard");
    return {};
});

describe("monaco-setup", () => {
    it("nạp ngôn ngữ PostgreSQL và các tính năng soạn code, gồm khung gợi ý khi gõ", async () => {
        await import("./monaco-setup");

        expect([...loaded].sort()).toEqual([
            "bracketMatching",
            "clipboard",
            "comment",
            "contextmenu",
            "find",
            "linesOperations",
            "pgsql",
            "suggest",
            "wordHighlighter",
        ]);
    });
});
