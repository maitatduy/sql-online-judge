"use client";

import { useState } from "react";

import { DIALECT_LABELS } from "@/lib/dialect";
import { cn } from "@/lib/utils";

import { useCodeRun } from "../hooks/useCodeRun";
import { useEditorDraft } from "../hooks/useEditorDraft";
import { useSubmission } from "../hooks/useSubmission";
import type { ProblemDetail } from "../types";
import { EditorPanel } from "./EditorPanel";
import { ProblemTabs } from "./ProblemTabs";
import { ResultPanel, type ResultTab } from "./ResultPanel";
import { RunContent } from "./RunContent";
import { SubmissionContent } from "./SubmissionContent";
import { WorkspaceActions } from "./WorkspaceActions";
import { type WorkspaceArea, WorkspaceAreaTabs, workspaceAreaId } from "./WorkspaceAreaTabs";
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
    const codeRun = useCodeRun(problem.id);
    const judging = useSubmission(problem.id);
    const [resultTab, setResultTab] = useState<ResultTab>("run");
    const [activeArea, setActiveArea] = useState<WorkspaceArea>("problem");
    const latestVerdict =
        judging.submission?.status === "COMPLETED" ? judging.submission.verdict : null;

    function visibleOnNarrowWhen(area: WorkspaceArea): string | false {
        return activeArea !== area && "max-lg:hidden";
    }

    function handleRun() {
        setResultTab("run");
        setActiveArea("result");
        if (!codeRun.isRunning) {
            codeRun.startRun({ dialect, code });
        }
    }

    function handleSubmit() {
        setResultTab("submission");
        setActiveArea("result");
        if (!judging.isJudging) {
            judging.submit({ dialect, code });
        }
    }

    return (
        <>
            <WorkspaceHeader title={`#${problem.number} · ${problem.title}`}>
                <WorkspaceActions
                    isRunning={codeRun.isRunning}
                    isJudging={judging.isJudging}
                    onRun={handleRun}
                    onSubmit={handleSubmit}
                />
            </WorkspaceHeader>
            <WorkspaceAreaTabs
                activeArea={activeArea}
                onAreaChange={setActiveArea}
                resultVerdict={latestVerdict}
            />
            <div className="flex min-h-0 flex-1 flex-col max-lg:pb-[68px] lg:flex-row">
                <ProblemTabs
                    problem={problem}
                    id={workspaceAreaId("problem")}
                    className={cn(visibleOnNarrowWhen("problem"))}
                />
                <div
                    className={cn(
                        "flex min-h-0 min-w-0 flex-1 flex-col",
                        activeArea === "problem" && "max-lg:hidden",
                    )}
                >
                    <div
                        id={workspaceAreaId("code")}
                        className={cn("flex min-h-0 flex-1 flex-col", visibleOnNarrowWhen("code"))}
                    >
                        <EditorPanel
                            engineLabel={problem.engineVersions[dialect] ?? DIALECT_LABELS[dialect]}
                            code={code}
                            savedAt={savedAt}
                            onChange={changeCode}
                            onReset={resetToStarter}
                        />
                    </div>
                    <ResultPanel
                        id={workspaceAreaId("result")}
                        className={cn(visibleOnNarrowWhen("result"))}
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
