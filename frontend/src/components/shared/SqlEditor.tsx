"use client";

import { Editor, type OnMount } from "@monaco-editor/react";
import { useRef } from "react";

import { type EditorBase, editorThemeFrom } from "@/lib/editor-theme";
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

interface SqlEditorProps {
    value: string;
    onChange: (value: string) => void;
    ariaLabel: string;
    readOnly?: boolean;
    className?: string;
}

export function SqlEditor({
    value,
    onChange,
    ariaLabel,
    readOnly = false,
    className,
}: SqlEditorProps) {
    const containerRef = useRef<HTMLDivElement>(null);

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
