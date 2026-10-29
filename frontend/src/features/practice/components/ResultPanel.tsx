import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

export type ResultTab = "run" | "submission";

interface ResultPanelProps {
    activeTab: ResultTab;
    onTabChange: (tab: ResultTab) => void;
    runContent: React.ReactNode;
    submissionContent: React.ReactNode;
}

export function ResultPanel({
    activeTab,
    onTabChange,
    runContent,
    submissionContent,
}: ResultPanelProps) {
    return (
        <Tabs
            value={activeTab}
            onValueChange={(tab) => onTabChange(tab as ResultTab)}
            aria-label="Kết quả"
            className="h-2/5 min-h-40 shrink-0 border-t bg-card"
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
