import { Database, History } from "lucide-react";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";

import type { WorkspaceProblem } from "../types";
import type { WorkspaceTarget } from "../workspace-target";
import { ProblemStatement } from "./ProblemStatement";
import { SchemaPanel } from "./SchemaPanel";
import { SubmissionHistory } from "./SubmissionHistory";

interface ProblemTabsProps {
    problem: WorkspaceProblem;
    target: WorkspaceTarget;
    id: string;
    className?: string;
}

export function ProblemTabs({ problem, target, id, className }: ProblemTabsProps) {
    return (
        <Tabs
            id={id}
            defaultValue="statement"
            aria-label="Đề bài và schema"
            className={cn(
                "min-h-0 max-lg:flex-1 lg:w-[38%] lg:min-w-[420px] lg:border-r",
                className,
            )}
        >
            <TabsList className="overflow-x-auto">
                <TabsTrigger value="statement">Đề bài</TabsTrigger>
                <TabsTrigger value="schema">
                    <Database aria-hidden="true" />
                    Schema
                </TabsTrigger>
                {target.historyProblemId ? (
                    <TabsTrigger value="history">
                        <History aria-hidden="true" />
                        Lịch sử nộp
                    </TabsTrigger>
                ) : null}
                {target.extraTabs?.map((tab) => (
                    <TabsTrigger key={tab.value} value={tab.value}>
                        {tab.label}
                    </TabsTrigger>
                ))}
            </TabsList>
            <TabsContent value="statement" className="overflow-y-auto">
                <ProblemStatement problem={problem} meta={target.statementMeta} />
            </TabsContent>
            <TabsContent value="schema" className="overflow-y-auto">
                <SchemaPanel tables={problem.schema} />
            </TabsContent>
            {target.historyProblemId ? (
                <TabsContent value="history" className="overflow-y-auto">
                    <SubmissionHistory
                        problemId={target.historyProblemId}
                        loginPath={target.loginPath}
                    />
                </TabsContent>
            ) : null}
            {target.extraTabs?.map((tab) => (
                <TabsContent key={tab.value} value={tab.value} className="overflow-y-auto">
                    {tab.content}
                </TabsContent>
            ))}
        </Tabs>
    );
}
