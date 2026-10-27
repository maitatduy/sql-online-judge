"use client";

import { Editor, type OnMount } from "@monaco-editor/react";
import { useEffect, useRef } from "react";

import { editorThemeFrom } from "@/lib/editor-theme";
import { cn } from "@/lib/utils";

import "./monaco-setup";

const THEME_NAME = "sqljudge-dark";
const EDITOR_LANGUAGE = "pgsql";
const EDITOR_FONT_SIZE = 14;
const EDITOR_LINE_HEIGHT = 22;

interface SqlEditorProps {
    value: string;
    onChange: (value: string) => void;
    ariaLabel: string;
    onRun?: () => void;
    onSubmit?: () => void;
    readOnly?: boolean;
    className?: string;
}

export function SqlEditor({
    value,
    onChange,
    ariaLabel,
    onRun,
    onSubmit,
    readOnly = false,
    className,
}: SqlEditorProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const shortcutsRef = useRef({ onRun, onSubmit });

    useEffect(() => {
        shortcutsRef.current = { onRun, onSubmit };
    }, [onRun, onSubmit]);

    const handleMount: OnMount = (editor, monaco) => {
        const style = getComputedStyle(containerRef.current ?? document.documentElement);
        monaco.editor.defineTheme(
            THEME_NAME,
            editorThemeFrom((name) => style.getPropertyValue(name)),
        );
        monaco.editor.setTheme(THEME_NAME);
        const fontFamily = style.getPropertyValue("--font-mono").trim();
        if (fontFamily) {
            editor.updateOptions({ fontFamily });
        }
        editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () =>
            shortcutsRef.current.onRun?.(),
        );
        editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.Enter, () =>
            shortcutsRef.current.onSubmit?.(),
        );
    };

    return (
        <div ref={containerRef} className={cn("h-full min-h-0", className)}>
            <Editor
                value={value}
                language={EDITOR_LANGUAGE}
                theme={THEME_NAME}
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
