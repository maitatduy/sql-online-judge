"use client";

import { Database, Play } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DIALECT_LABELS } from "@/lib/dialect";

import { useCodeRun } from "../hooks/useCodeRun";
import { useEditorDraft } from "../hooks/useEditorDraft";
import type { ProblemDetail } from "../types";
import { EditorPanel } from "./EditorPanel";
import { ProblemStatement } from "./ProblemStatement";
import { RunPanel } from "./RunPanel";
import { SchemaPanel } from "./SchemaPanel";
import { WorkspaceHeader } from "./WorkspaceHeader";

interface ProblemWorkspaceProps {
    problem: ProblemDetail;
}

export function ProblemWorkspace({ problem }: ProblemWorkspaceProps) {
    const [dialect] = problem.dialects;
    const { code, savedAt, changeCode, resetToStarter } = useEditorDraft(
        problem.id,
        problem.starterCode,
    );
    const { run, isRunning, error, startRun } = useCodeRun(problem.id);

    function handleRun() {
        if (!isRunning) {
            startRun({ dialect, code });
        }
    }

    return (
        <>
            <WorkspaceHeader title={`#${problem.number} · ${problem.title}`}>
                <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={isRunning}
                    aria-keyshortcuts="Control+Enter"
                    onClick={handleRun}
                >
                    <Play aria-hidden="true" />
                    Run
                    <kbd
                        aria-hidden="true"
                        className="rounded-sm bg-surface-2 px-1.5 font-mono text-[11px] text-muted-foreground max-md:hidden"
                    >
                        Ctrl ↵
                    </kbd>
                </Button>
            </WorkspaceHeader>
            <div className="flex min-h-0 flex-1 flex-col lg:flex-row">
                <Tabs
                    defaultValue="statement"
                    aria-label="Đề bài và schema"
                    className="min-h-0 border-b lg:w-[38%] lg:min-w-[420px] lg:border-r lg:border-b-0"
                >
                    <TabsList>
                        <TabsTrigger value="statement">Đề bài</TabsTrigger>
                        <TabsTrigger value="schema">
                            <Database aria-hidden="true" />
                            Schema
                        </TabsTrigger>
                    </TabsList>
                    <TabsContent value="statement" className="overflow-y-auto">
                        <ProblemStatement problem={problem} />
                    </TabsContent>
                    <TabsContent value="schema" className="overflow-y-auto">
                        <SchemaPanel tables={problem.schema} />
                    </TabsContent>
                </Tabs>
                <div className="flex min-h-[560px] min-w-0 flex-1 flex-col">
                    <EditorPanel
                        engineLabel={problem.engineVersions[dialect] ?? DIALECT_LABELS[dialect]}
                        code={code}
                        savedAt={savedAt}
                        onChange={changeCode}
                        onReset={resetToStarter}
                        onRun={handleRun}
                    />
                    <RunPanel
                        problem={problem}
                        run={run}
                        isRunning={isRunning}
                        error={error}
                        onRetry={handleRun}
                    />
                </div>
            </div>
        </>
    );
}
