"use client";

import { Play } from "lucide-react";

import { Markdown } from "@/components/shared/Markdown";
import { SqlRunResult } from "@/components/shared/SqlRunResult";
import { Button } from "@/components/ui/button";
import { DIALECT_LABELS } from "@/lib/dialect";

import { useLinkedProblem } from "../hooks/useCommunity";
import { useProblemRun } from "../hooks/useProblemRun";
import type { LinkedProblem } from "../types";

const SQL_RUN_FENCE = /^```sql run[ \t]*$/gm;
const BODY_CLASSES =
    "text-base [&_h2]:mt-2 [&_h2]:text-xl [&_h2]:font-bold [&_h3]:text-lg [&_h3]:font-bold [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:border [&_pre]:bg-surface-2 [&_pre]:p-4 [&_pre_code]:bg-transparent [&_pre_code]:p-0";

interface PostBodyProps {
    markdown: string;
    problemSlug: string | null;
}

export function PostBody({ markdown, problemSlug }: PostBodyProps) {
    const { data: problem } = useLinkedProblem(problemSlug);
    const source = markdown.replace(SQL_RUN_FENCE, "```sql-run");

    return (
        <Markdown
            className={BODY_CLASSES}
            renderSqlRun={(sql) => <RunnableSql sql={sql} problem={problem} />}
        >
            {source}
        </Markdown>
    );
}

interface RunnableSqlProps {
    sql: string;
    problem: LinkedProblem | undefined;
}

function RunnableSql({ sql, problem }: RunnableSqlProps) {
    const dialect = problem?.dialects[0] ?? null;
    const { run, isRunning, error, startRun } = useProblemRun(problem?.id ?? "", dialect);
    const hasOutput = isRunning || error !== null || run !== null;

    return (
        <section aria-label="Khối SQL chạy được" className="overflow-hidden rounded-lg border">
            <div className="flex items-center justify-between gap-2 border-b bg-card px-3 py-2">
                <span className="text-sm font-semibold text-muted-foreground">
                    {problem
                        ? `${problem.title} - ${DIALECT_LABELS[dialect ?? "POSTGRESQL"]}`
                        : "SQL"}
                </span>
                {problem ? (
                    <Button
                        type="button"
                        size="sm"
                        disabled={isRunning}
                        onClick={() => startRun(sql)}
                    >
                        <Play aria-hidden="true" />
                        Chạy
                    </Button>
                ) : null}
            </div>
            <pre className="!rounded-none !border-0 font-mono text-sm">
                <code>{sql}</code>
            </pre>
            {hasOutput ? (
                <div className="border-t bg-card px-4 py-3">
                    <SqlRunResult run={run} isRunning={isRunning} error={error} />
                </div>
            ) : null}
        </section>
    );
}
