"use client";

import { Editor, type Monaco, type OnMount } from "@monaco-editor/react";
import type { editor as MonacoEditor, Position } from "monaco-editor";
import { type RefObject, useEffect, useRef } from "react";

import { type EditorBase, editorThemeFrom } from "@/lib/editor-theme";
import {
    type CompletionSchema,
    type SqlSuggestionKind,
    sqlSuggestions,
} from "@/lib/sql-completion";
import { cn } from "@/lib/utils";

import "./monaco-setup";

const THEME_NAMES: Record<EditorBase, string> = {
    vs: "sqljudge-light",
    "vs-dark": "sqljudge-dark",
};
const DARK_SURFACE_SELECTOR = ".dark";
const EDITOR_LANGUAGE = "pgsql";
const EDITOR_FONT_SIZE = 14;
const EDITOR_LINE_HEIGHT = 22;
const COMPLETION_TRIGGERS = ["."];

type CodeEditor = Parameters<OnMount>[0];

interface SqlEditorProps {
    value: string;
    onChange: (value: string) => void;
    ariaLabel: string;
    readOnly?: boolean;
    schema?: CompletionSchema;
    className?: string;
}

function completionKindOf(monaco: Monaco, kind: SqlSuggestionKind) {
    const kinds = monaco.languages.CompletionItemKind;
    if (kind === "table") {
        return kinds.Struct;
    }
    return kind === "column" ? kinds.Field : kinds.Keyword;
}

function registerCompletions(
    editor: CodeEditor,
    monaco: Monaco,
    schemaRef: RefObject<CompletionSchema | undefined>,
) {
    const provider = monaco.languages.registerCompletionItemProvider(EDITOR_LANGUAGE, {
        triggerCharacters: COMPLETION_TRIGGERS,
        provideCompletionItems: (model: MonacoEditor.ITextModel, position: Position) => {
            const schema = schemaRef.current;
            if (model !== editor.getModel() || !schema) {
                return { suggestions: [] };
            }
            const word = model.getWordUntilPosition(position);
            const range = {
                startLineNumber: position.lineNumber,
                endLineNumber: position.lineNumber,
                startColumn: word.startColumn,
                endColumn: word.endColumn,
            };
            const textBeforeCursor = model.getValueInRange({
                startLineNumber: 1,
                startColumn: 1,
                endLineNumber: position.lineNumber,
                endColumn: word.startColumn,
            });
            const suggestions = sqlSuggestions(schema, {
                textBeforeCursor,
                fullText: model.getValue(),
            });
            return {
                suggestions: suggestions.map((suggestion) => ({
                    label: suggestion.label,
                    kind: completionKindOf(monaco, suggestion.kind),
                    detail: suggestion.detail,
                    insertText: suggestion.label,
                    range,
                })),
            };
        },
    });
    editor.onDidDispose(() => provider.dispose());
}

export function SqlEditor({
    value,
    onChange,
    ariaLabel,
    readOnly = false,
    schema,
    className,
}: SqlEditorProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const schemaRef = useRef(schema);

    useEffect(() => {
        schemaRef.current = schema;
    }, [schema]);

    const handleMount: OnMount = (editor, monaco) => {
        const container = containerRef.current ?? document.documentElement;
        const style = getComputedStyle(container);
        const base: EditorBase = container.closest(DARK_SURFACE_SELECTOR) ? "vs-dark" : "vs";
        monaco.editor.defineTheme(
            THEME_NAMES[base],
            editorThemeFrom((name) => style.getPropertyValue(name), base),
        );
        monaco.editor.setTheme(THEME_NAMES[base]);
        const fontFamily = style.getPropertyValue("--font-mono").trim();
        if (fontFamily) {
            editor.updateOptions({ fontFamily });
        }
        if (schemaRef.current) {
            registerCompletions(editor, monaco, schemaRef);
        }
    };

    return (
        <div ref={containerRef} className={cn("h-full min-h-0", className)}>
            <Editor
                value={value}
                language={EDITOR_LANGUAGE}
                onChange={(nextValue) => onChange(nextValue ?? "")}
                onMount={handleMount}
                options={{
                    ariaLabel,
                    readOnly,
                    fontSize: EDITOR_FONT_SIZE,
                    lineHeight: EDITOR_LINE_HEIGHT,
                    minimap: { enabled: false },
                    scrollBeyondLastLine: false,
                    automaticLayout: true,
                    tabSize: 4,
                }}
            />
        </div>
    );
}
