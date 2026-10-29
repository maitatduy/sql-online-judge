"use client";

import { useState } from "react";

import { Database, Play, Send } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DIALECT_LABELS } from "@/lib/dialect";

import { useCodeRun } from "../hooks/useCodeRun";
import { useEditorDraft } from "../hooks/useEditorDraft";
import { useSubmission } from "../hooks/useSubmission";
import type { ProblemDetail } from "../types";
import { EditorPanel } from "./EditorPanel";
import { ProblemStatement } from "./ProblemStatement";
import { ResultPanel, type ResultTab } from "./ResultPanel";
import { RunContent } from "./RunContent";
import { SchemaPanel } from "./SchemaPanel";
import { SubmissionContent } from "./SubmissionContent";
import { WorkspaceHeader } from "./WorkspaceHeader";

const SHORTCUT_CLASS =
    "rounded-sm bg-surface-2 px-1.5 font-mono text-[11px] text-muted-foreground max-md:hidden";

interface ProblemWorkspaceProps {
    problem: ProblemDetail;
}

export function ProblemWorkspace({ problem }: ProblemWorkspaceProps) {
    const [dialect] = problem.dialects;
    const { code, savedAt, changeCode, resetToStarter } = useEditorDraft(
        problem.id,
        problem.starterCode,
    );
    const codeRun = useCodeRun(problem.id);
    const judging = useSubmission(problem.id);
    const [resultTab, setResultTab] = useState<ResultTab>("run");

    function handleRun() {
        setResultTab("run");
        if (!codeRun.isRunning) {
            codeRun.startRun({ dialect, code });
        }
    }

    function handleSubmit() {
        setResultTab("submission");
        if (!judging.isJudging) {
            judging.submit({ dialect, code });
        }
    }

    return (
        <>
            <WorkspaceHeader title={`#${problem.number} · ${problem.title}`}>
                <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={codeRun.isRunning}
                    aria-keyshortcuts="Control+Enter"
                    onClick={handleRun}
                >
                    <Play aria-hidden="true" />
                    Run
                    <kbd aria-hidden="true" className={SHORTCUT_CLASS}>
                        Ctrl ↵
                    </kbd>
                </Button>
                <Button
                    type="button"
                    size="sm"
                    disabled={judging.isJudging}
                    aria-keyshortcuts="Control+Shift+Enter"
                    onClick={handleSubmit}
                >
                    <Send aria-hidden="true" />
                    Submit
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
                        onSubmit={handleSubmit}
                    />
                    <ResultPanel
                        activeTab={resultTab}
                        onTabChange={setResultTab}
                        runContent={
                            <RunContent
                                problem={problem}
                                run={codeRun.run}
                                isRunning={codeRun.isRunning}
                                error={codeRun.error}
                                onRetry={handleRun}
                            />
                        }
                        submissionContent={
                            <SubmissionContent
                                problem={problem}
                                submission={judging.submission}
                                isJudging={judging.isJudging}
                                error={judging.error}
                                onRetry={handleSubmit}
                            />
                        }
                    />
                </div>
            </div>
        </>
    );
}
