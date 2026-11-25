import { Database, FileText, History } from "lucide-react";
import { useState } from "react";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

import type { WorkspaceProblem } from "../types";
import type { WorkspaceTarget } from "../workspace-target";
import { ProblemStatement } from "./ProblemStatement";
import { SchemaPanel } from "./SchemaPanel";
import { SubmissionHistory } from "./SubmissionHistory";

interface ProblemTabsProps {
    problem: WorkspaceProblem;
    target: WorkspaceTarget;
}

interface PanelTab {
    value: string;
    label: string;
    icon: React.ReactNode;
    content: React.ReactNode;
}

const DEFAULT_TAB = "statement";
const TAB_PARAM = "tab";

function initialTab(target: WorkspaceTarget): string {
    const requested =
        typeof window === "undefined"
            ? null
            : new URLSearchParams(window.location.search).get(TAB_PARAM);
    const isKnown = target.extraTabs?.some((tab) => tab.value === requested) ?? false;
    return requested && isKnown ? requested : DEFAULT_TAB;
}

function tabsOf(problem: WorkspaceProblem, target: WorkspaceTarget): PanelTab[] {
    const history: PanelTab[] = target.historyProblemId
        ? [
              {
                  value: "history",
                  label: "Lịch sử nộp",
                  icon: <History aria-hidden="true" />,
                  content: (
                      <SubmissionHistory
                          problemId={target.historyProblemId}
                          loginPath={target.loginPath}
                      />
                  ),
              },
          ]
        : [];
    return [
        {
            value: "statement",
            label: "Đề bài",
            icon: <FileText aria-hidden="true" />,
            content: <ProblemStatement problem={problem} meta={target.statementMeta} />,
        },
        {
            value: "schema",
            label: "Schema",
            icon: <Database aria-hidden="true" />,
            content: <SchemaPanel tables={problem.schema} />,
        },
        ...history,
        ...(target.extraTabs ?? []),
    ];
}

export function ProblemTabs({ problem, target }: ProblemTabsProps) {
    const [defaultTab] = useState(() => initialTab(target));
    const tabs = tabsOf(problem, target);

    return (
        <Tabs
            defaultValue={defaultTab}
            aria-label="Đề bài và schema"
            className="@container/tabs h-full min-h-0 flex-1"
        >
            <TabsList className="gap-0.5 px-1">
                {tabs.map((tab) => (
                    <TabsTrigger
                        key={tab.value}
                        value={tab.value}
                        aria-label={tab.label}
                        title={tab.label}
                        className="h-11 shrink-0 gap-2 px-3 text-[15px] [&_svg]:size-[18px]"
                    >
                        {tab.icon}
                        <span className="hidden @[46rem]/tabs:inline">{tab.label}</span>
                    </TabsTrigger>
                ))}
            </TabsList>
            {tabs.map((tab) => (
                <TabsContent key={tab.value} value={tab.value} className="overflow-y-auto">
                    {tab.content}
                </TabsContent>
            ))}
        </Tabs>
    );
}
