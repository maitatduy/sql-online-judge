const HEX_COLOR = /^#[0-9a-f]{6}$/;

const COLOR_TOKENS = {
    "editor.background": "--bg",
    "editor.foreground": "--text",
    "editorLineNumber.foreground": "--text-muted",
    "editorLineNumber.activeForeground": "--text",
    "editorCursor.foreground": "--brand",
    "editor.lineHighlightBackground": "--surface-2",
    "editorWidget.border": "--line",
} as const;

const SYNTAX_RULES = [
    { token: "keyword", cssVariable: "--syntax-keyword", fontStyle: "bold" },
    { token: "predefined", cssVariable: "--syntax-function" },
    { token: "string", cssVariable: "--syntax-string" },
    { token: "number", cssVariable: "--syntax-number" },
    { token: "comment", cssVariable: "--syntax-comment", fontStyle: "italic" },
] as const;

interface EditorThemeRule {
    token: string;
    foreground: string;
    fontStyle?: string;
}

export type EditorBase = "vs" | "vs-dark";

interface EditorTheme {
    base: EditorBase;
    inherit: true;
    colors: Record<string, string>;
    rules: EditorThemeRule[];
}

type ReadToken = (cssVariable: string) => string;

function hexOf(readToken: ReadToken, cssVariable: string): string | null {
    const value = readToken(cssVariable).trim().toLowerCase();
    return HEX_COLOR.test(value) ? value : null;
}

export function editorThemeFrom(readToken: ReadToken, base: EditorBase): EditorTheme {
    const colors = Object.fromEntries(
        Object.entries(COLOR_TOKENS).flatMap(([colorKey, cssVariable]) => {
            const hex = hexOf(readToken, cssVariable);
            return hex ? [[colorKey, hex]] : [];
        }),
    );
    const rules = SYNTAX_RULES.flatMap(({ token, cssVariable, ...style }) => {
        const hex = hexOf(readToken, cssVariable);
        return hex ? [{ token, foreground: hex.slice(1), ...style }] : [];
    });
    return { base, inherit: true, colors, rules };
}
