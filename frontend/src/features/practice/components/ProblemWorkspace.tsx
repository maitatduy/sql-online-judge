"use client";

import { useMemo, useState } from "react";

import type { CompletionSchema } from "@/lib/sql-completion";
import { useMediaQuery } from "@/lib/useMediaQuery";
import { cn } from "@/lib/utils";

import { useCodeRun } from "../hooks/useCodeRun";
import { useDialectChoice } from "../hooks/useDialectChoice";
import { useEditorDraft } from "../hooks/useEditorDraft";
import { useSubmission } from "../hooks/useSubmission";
import type { WorkspaceProblem } from "../types";
import type { WorkspaceTarget } from "../workspace-target";
import { EditorPanel } from "./EditorPanel";
import { ProblemTabs } from "./ProblemTabs";
import { ResultPanel, type ResultTab } from "./ResultPanel";
import { RunContent } from "./RunContent";
import { SubmissionContent } from "./SubmissionContent";
import { WorkspaceActions } from "./WorkspaceActions";
import { type WorkspaceArea, WorkspaceAreaTabs, workspaceAreaId } from "./WorkspaceAreaTabs";
import { WorkspaceHeader } from "./WorkspaceHeader";
import { WorkspaceSplitLayout } from "./WorkspaceSplitLayout";

const DESKTOP_QUERY = "(min-width: 1024px)";

interface ProblemWorkspaceProps {
    problem: WorkspaceProblem;
    target: WorkspaceTarget;
    header?: React.ReactNode;
}

export function ProblemWorkspace({ problem, target, header }: ProblemWorkspaceProps) {
    const { dialect, chooseDialect } = useDialectChoice(target.storageKey, problem.dialects);
    const { code, savedAt, changeCode, resetToStarter } = useEditorDraft(
        target.storageKey,
        dialect,
        problem.starterCode,
    );
    const codeRun = useCodeRun(target.startRun);
    const judging = useSubmission(target.submit, target.historyProblemId);
    const [resultTab, setResultTab] = useState<ResultTab>("run");
    const [activeArea, setActiveArea] = useState<WorkspaceArea>("problem");
    const completionSchema = useMemo<CompletionSchema>(
        () => ({
            tables: problem.schema.map((table) => ({
                name: table.name,
                columns: table.columns.map((column) => column.name),
            })),
        }),
        [problem.schema],
    );
    const isDesktop = useMediaQuery(DESKTOP_QUERY);
    const latestVerdict =
        judging.submission?.status === "COMPLETED" ? judging.submission.verdict : null;

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

    function narrowAreaClass(area: WorkspaceArea): string {
        return cn("flex min-h-0 flex-1 flex-col", activeArea !== area && "hidden");
    }

    const problemPane = <ProblemTabs problem={problem} target={target} />;
    const editorPane = (
        <EditorPanel
            dialects={problem.dialects}
            dialect={dialect}
            engineVersions={problem.engineVersions}
            onDialectChange={chooseDialect}
            code={code}
            schema={completionSchema}
            savedAt={savedAt}
            onChange={changeCode}
            onReset={resetToStarter}
        />
    );
    const resultPane = (
        <ResultPanel
            activeTab={resultTab}
            onTabChange={setResultTab}
            runContent={
                <RunContent
                    problem={problem}
                    loginPath={target.loginPath}
                    run={codeRun.run}
                    isRunning={codeRun.isRunning}
                    error={codeRun.error}
                    onRetry={handleRun}
                />
            }
            submissionContent={
                <SubmissionContent
                    problem={problem}
                    loginPath={target.loginPath}
                    submission={judging.submission}
                    isJudging={judging.isJudging}
                    error={judging.error}
                    onRetry={handleSubmit}
                />
            }
        />
    );

    return (
        <>
            <WorkspaceHeader parent={target.parent} title={target.heading}>
                <WorkspaceActions
                    isRunning={codeRun.isRunning}
                    isJudging={judging.isJudging}
                    onRun={handleRun}
                    onSubmit={handleSubmit}
                />
            </WorkspaceHeader>
            {header}
            {isDesktop ? null : (
                <WorkspaceAreaTabs
                    activeArea={activeArea}
                    onAreaChange={setActiveArea}
                    resultVerdict={latestVerdict}
                />
            )}
            {isDesktop ? (
                <WorkspaceSplitLayout
                    problemPane={problemPane}
                    editorPane={editorPane}
                    resultPane={resultPane}
                />
            ) : (
                <div className="flex min-h-0 flex-1 flex-col pb-[68px]">
                    <div id={workspaceAreaId("problem")} className={narrowAreaClass("problem")}>
                        {problemPane}
                    </div>
                    <div id={workspaceAreaId("code")} className={narrowAreaClass("code")}>
                        {editorPane}
                    </div>
                    <div id={workspaceAreaId("result")} className={narrowAreaClass("result")}>
                        {resultPane}
                    </div>
                </div>
            )}
        </>
    );
}
