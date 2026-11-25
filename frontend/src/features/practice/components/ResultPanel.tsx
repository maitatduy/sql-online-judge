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
            className="h-full min-h-0 flex-1 bg-card"
        >
            <TabsList>
                <TabsTrigger value="run" className="h-9">
                    Chạy thử
                </TabsTrigger>
                <TabsTrigger value="submission" className="h-9">
                    Nộp bài
                </TabsTrigger>
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
