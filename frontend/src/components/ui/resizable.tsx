"use client";

import { Group, Panel, Separator } from "react-resizable-panels";

import { cn } from "@/lib/utils";

function ResizableGroup({ className, ...props }: React.ComponentProps<typeof Group>) {
    return (
        <Group data-slot="resizable-group" className={cn("h-full w-full", className)} {...props} />
    );
}

function ResizablePanel(props: React.ComponentProps<typeof Panel>) {
    return <Panel data-slot="resizable-panel" {...props} />;
}

function ResizableHandle({ className, ...props }: React.ComponentProps<typeof Separator>) {
    return (
        <Separator
            data-slot="resizable-handle"
            className={cn(
                "shrink-0 bg-border transition-colors outline-none aria-[orientation=horizontal]:h-1 aria-[orientation=vertical]:w-1 data-[separator=active]:bg-brand data-[separator=focus]:bg-brand data-[separator=hover]:bg-brand/50",
                className,
            )}
            {...props}
        />
    );
}

export { ResizableGroup, ResizableHandle, ResizablePanel };
