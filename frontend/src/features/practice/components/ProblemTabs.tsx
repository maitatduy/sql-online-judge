import { Database, History } from "lucide-react";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";

import type { ProblemDetail } from "../types";
import { ProblemStatement } from "./ProblemStatement";
import { SchemaPanel } from "./SchemaPanel";
import { SubmissionHistory } from "./SubmissionHistory";

interface ProblemTabsProps {
    problem: ProblemDetail;
    id: string;
    className?: string;
}

export function ProblemTabs({ problem, id, className }: ProblemTabsProps) {
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
            <TabsList>
                <TabsTrigger value="statement">Đề bài</TabsTrigger>
                <TabsTrigger value="schema">
                    <Database aria-hidden="true" />
                    Schema
                </TabsTrigger>
                <TabsTrigger value="history">
                    <History aria-hidden="true" />
                    Lịch sử nộp
                </TabsTrigger>
            </TabsList>
            <TabsContent value="statement" className="overflow-y-auto">
                <ProblemStatement problem={problem} />
            </TabsContent>
            <TabsContent value="schema" className="overflow-y-auto">
                <SchemaPanel tables={problem.schema} />
            </TabsContent>
            <TabsContent value="history" className="overflow-y-auto">
                <SubmissionHistory problem={problem} />
            </TabsContent>
        </Tabs>
    );
}
