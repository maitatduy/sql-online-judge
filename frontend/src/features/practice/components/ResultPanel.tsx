import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";

export type ResultTab = "run" | "submission";

interface ResultPanelProps {
    activeTab: ResultTab;
    onTabChange: (tab: ResultTab) => void;
    runContent: React.ReactNode;
    submissionContent: React.ReactNode;
    id: string;
    className?: string;
}

export function ResultPanel({
    activeTab,
    onTabChange,
    runContent,
    submissionContent,
    id,
    className,
}: ResultPanelProps) {
    return (
        <Tabs
            value={activeTab}
            onValueChange={(tab) => onTabChange(tab as ResultTab)}
            id={id}
            aria-label="Kết quả"
            className={cn(
                "shrink-0 border-t bg-card max-lg:flex-1 max-lg:border-t-0 lg:h-2/5 lg:min-h-40",
                className,
            )}
        >
            <TabsList>
                <TabsTrigger value="run">Chạy thử</TabsTrigger>
                <TabsTrigger value="submission">Nộp bài</TabsTrigger>
            </TabsList>
            <TabsContent value="run" className="overflow-auto px-4 py-3">
                {runContent}
            </TabsContent>
            <TabsContent value="submission" className="overflow-auto px-4 py-3">
                {submissionContent}
            </TabsContent>
        </Tabs>
    );
}
