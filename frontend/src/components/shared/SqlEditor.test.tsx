import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { SqlEditor } from "./SqlEditor";

interface CapturedEditorProps {
    value?: string;
    language?: string;
    theme?: string;
    options?: { ariaLabel?: string; readOnly?: boolean };
    onChange?: (value: string | undefined) => void;
    onMount?: (editor: FakeEditor, monaco: FakeMonaco) => void;
}

interface FakeEditor {
    addCommand: (keybinding: number, handler: () => void) => void;
    updateOptions: (options: { fontFamily?: string }) => void;
    getModel?: () => unknown;
    onDidDispose?: (listener: () => void) => void;
}

interface FakeMonaco {
    editor: {
        defineTheme: (name: string, theme: unknown) => void;
        setTheme: (name: string) => void;
    };
    languages?: {
        CompletionItemKind: Record<string, number>;
        registerCompletionItemProvider: (
            language: string,
            provider: CompletionProvider,
        ) => { dispose: () => void } | void;
    };
}

interface CompletionProvider {
    triggerCharacters: string[];
    provideCompletionItems: (
        model: unknown,
        position: { lineNumber: number; column: number },
    ) => { suggestions: { label: string; kind: number; detail: string }[] };
}

const captured = vi.hoisted(() => ({ props: {} as CapturedEditorProps }));

vi.mock("./monaco-setup", () => ({}));

vi.mock("@monaco-editor/react", () => ({
    Editor: (props: CapturedEditorProps) => {
        captured.props = props;
        return <div data-testid="monaco" />;
    },
}));

function fakeMonaco() {
    const commands = new Map<number, () => void>();
    const editor: FakeEditor = {
        addCommand: (keybinding, handler) => commands.set(keybinding, handler),
        updateOptions: vi.fn(),
    };
    const monaco: FakeMonaco = {
        editor: { defineTheme: vi.fn(), setTheme: vi.fn() },
    };
    return { editor, monaco, commands };
}

function mountEditor() {
    const fake = fakeMonaco();
    captured.props.onMount?.(fake.editor, fake.monaco);
    return fake;
}

describe("SqlEditor", () => {
    beforeEach(() => {
        captured.props = {};
    });

    it("mở Monaco với ngôn ngữ PostgreSQL, giá trị và nhãn truy cập", () => {
        render(<SqlEditor value="SELECT 1;" onChange={vi.fn()} ariaLabel="Mã SQL của bạn" />);

        expect(screen.getByTestId("monaco")).toBeInTheDocument();
        expect(captured.props.language).toBe("pgsql");
        expect(captured.props.value).toBe("SELECT 1;");
        expect(captured.props.options?.ariaLabel).toBe("Mã SQL của bạn");
    });

    it("báo nội dung mới khi người dùng gõ, coi giá trị rỗng là chuỗi rỗng", () => {
        const handleChange = vi.fn();
        render(<SqlEditor value="" onChange={handleChange} ariaLabel="Mã SQL" />);

        captured.props.onChange?.("SELECT 2;");
        captured.props.onChange?.(undefined);

        expect(handleChange.mock.calls).toEqual([["SELECT 2;"], [""]]);
    });

    it("không gắn phím tắt nào vào editor", () => {
        render(<SqlEditor value="" onChange={vi.fn()} ariaLabel="Mã SQL" />);

        const { commands } = mountEditor();

        expect(commands.size).toBe(0);
    });

    it("dùng theme sáng khi editor nằm trên nền sáng", () => {
        render(<SqlEditor value="" onChange={vi.fn()} ariaLabel="Mã SQL" />);

        const { monaco } = mountEditor();

        expect(monaco.editor.defineTheme).toHaveBeenCalledWith(
            "sqljudge-light",
            expect.objectContaining({ base: "vs" }),
        );
        expect(monaco.editor.setTheme).toHaveBeenCalledWith("sqljudge-light");
    });

    it("dùng theme tối khi editor nằm trong vùng nền tối", () => {
        render(
            <div className="dark">
                <SqlEditor value="" onChange={vi.fn()} ariaLabel="Mã SQL" />
            </div>,
        );

        const { monaco } = mountEditor();

        expect(monaco.editor.defineTheme).toHaveBeenCalledWith(
            "sqljudge-dark",
            expect.objectContaining({ base: "vs-dark" }),
        );
        expect(monaco.editor.setTheme).toHaveBeenCalledWith("sqljudge-dark");
    });

    it("dùng font mono của design system theo giá trị đã tính để Monaco đo đúng ký tự", () => {
        document.documentElement.style.setProperty("--font-mono", "JetBrains Mono, monospace");
        render(<SqlEditor value="" onChange={vi.fn()} ariaLabel="Mã SQL" />);

        const { editor } = mountEditor();

        expect(editor.updateOptions).toHaveBeenCalledWith({
            fontFamily: "JetBrains Mono, monospace",
        });
        document.documentElement.style.removeProperty("--font-mono");
    });

    it("có schema thì gợi ý cột khi gõ sau dấu chấm, gỡ gợi ý khi editor đóng", () => {
        render(
            <SqlEditor
                value=""
                onChange={vi.fn()}
                ariaLabel="Mã SQL"
                schema={{ tables: [{ name: "departments", columns: ["id", "name"] }] }}
            />,
        );
        const fake = fakeMonaco();
        const provider: { current?: CompletionProvider } = {};
        const dispose = vi.fn();
        const disposers: (() => void)[] = [];
        const model = {
            getWordUntilPosition: () => ({ startColumn: 20, endColumn: 20 }),
            getValueInRange: () => "SELECT departments.",
            getValue: () => "SELECT departments.",
        };
        const editor = {
            ...fake.editor,
            getModel: () => model,
            onDidDispose: (listener: () => void) => disposers.push(listener),
        };
        const monaco = {
            ...fake.monaco,
            languages: {
                CompletionItemKind: { Keyword: 17, Struct: 6, Field: 3 },
                registerCompletionItemProvider: (language: string, next: CompletionProvider) => {
                    expect(language).toBe("pgsql");
                    provider.current = next;
                    return { dispose };
                },
            },
        };

        captured.props.onMount?.(editor, monaco);
        const position = { lineNumber: 1, column: 20 };
        const result = provider.current?.provideCompletionItems(model, position);
        const otherModel = provider.current?.provideCompletionItems({ ...model }, position);
        disposers.forEach((listener) => listener());

        expect(provider.current?.triggerCharacters).toEqual(["."]);
        expect(result?.suggestions.map((suggestion) => suggestion.label)).toEqual(["id", "name"]);
        expect(result?.suggestions[0]).toMatchObject({ kind: 3, detail: "departments" });
        expect(otherModel?.suggestions).toEqual([]);
        expect(dispose).toHaveBeenCalled();
    });

    it("không có schema thì không đăng ký gợi ý", () => {
        render(<SqlEditor value="" onChange={vi.fn()} ariaLabel="Mã SQL" />);
        const registerCompletionItemProvider = vi.fn();
        const fake = fakeMonaco();

        captured.props.onMount?.(fake.editor, {
            ...fake.monaco,
            languages: { CompletionItemKind: {}, registerCompletionItemProvider },
        });

        expect(registerCompletionItemProvider).not.toHaveBeenCalled();
    });

    it("chuyển sang chế độ chỉ đọc khi được yêu cầu", () => {
        render(<SqlEditor value="SELECT 1;" onChange={vi.fn()} ariaLabel="Mã đã nộp" readOnly />);

        expect(captured.props.options?.readOnly).toBe(true);
    });
});
