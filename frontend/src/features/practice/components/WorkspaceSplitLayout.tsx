"use client";

import { useDefaultLayout } from "react-resizable-panels";

import { ResizableGroup, ResizableHandle, ResizablePanel } from "@/components/ui/resizable";

interface WorkspaceSplitLayoutProps {
    problemPane: React.ReactNode;
    editorPane: React.ReactNode;
    resultPane: React.ReactNode;
}

const COLUMNS_ID = "sqljudge-workspace-columns";
const ROWS_ID = "sqljudge-workspace-rows";
const PANE_CLASS = "flex h-full min-h-0 flex-col";

export function WorkspaceSplitLayout({
    problemPane,
    editorPane,
    resultPane,
}: WorkspaceSplitLayoutProps) {
    const columns = useDefaultLayout({ id: COLUMNS_ID });
    const rows = useDefaultLayout({ id: ROWS_ID });

    return (
        <ResizableGroup
            id={COLUMNS_ID}
            orientation="horizontal"
            defaultLayout={columns.defaultLayout}
            onLayoutChanged={columns.onLayoutChanged}
            className="min-h-0 flex-1"
        >
            <ResizablePanel id="problem" defaultSize="38" minSize={320}>
                <div className={PANE_CLASS}>{problemPane}</div>
            </ResizablePanel>
            <ResizableHandle aria-label="Kéo để đổi độ rộng khung đề bài" />
            <ResizablePanel id="work" minSize={420}>
                <ResizableGroup
                    id={ROWS_ID}
                    orientation="vertical"
                    defaultLayout={rows.defaultLayout}
                    onLayoutChanged={rows.onLayoutChanged}
                >
                    <ResizablePanel id="editor" defaultSize="60" minSize={160}>
                        <div className={PANE_CLASS}>{editorPane}</div>
                    </ResizablePanel>
                    <ResizableHandle aria-label="Kéo để đổi chiều cao khung kết quả" />
                    <ResizablePanel id="result" defaultSize="40" minSize={120}>
                        <div className={PANE_CLASS}>{resultPane}</div>
                    </ResizablePanel>
                </ResizableGroup>
            </ResizablePanel>
        </ResizableGroup>
    );
}
