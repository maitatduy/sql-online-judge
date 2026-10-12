import { Table2 } from "lucide-react";

import { cn } from "@/lib/utils";

type ColumnKey = "PK" | "FK";

export interface SchemaColumn {
    name: string;
    type: string;
    key?: ColumnKey;
    isHighlighted?: boolean;
    typeClassName?: string;
}

interface SchemaTableCardProps {
    name: string;
    columns: SchemaColumn[];
    className?: string;
}

const KEY_CLASSES: Record<ColumnKey, string> = {
    PK: "text-verdict-tle",
    FK: "text-syntax-function",
};

export function SchemaTableCard({ name, columns, className }: SchemaTableCardProps) {
    return (
        <div
            className={cn(
                "dark absolute w-[236px] overflow-hidden rounded-lg border bg-surface font-mono text-foreground shadow-2xl",
                className,
            )}
        >
            <div className="flex items-center gap-2 border-b bg-surface-2 px-3.5 py-2.5 text-[13px] font-bold">
                <Table2 className="size-4 text-syntax-function" />
                {name}
            </div>
            {columns.map((column) => (
                <div
                    key={column.name}
                    className={cn(
                        "flex items-center gap-2 border-t px-3.5 py-1.5 text-xs text-muted-foreground first:border-t-0",
                        column.isHighlighted && "bg-brand-soft text-foreground",
                    )}
                >
                    <span
                        className={cn(
                            "w-5 text-[10px] font-bold",
                            column.key && KEY_CLASSES[column.key],
                        )}
                    >
                        {column.key}
                    </span>
                    {column.name}
                    <span className={cn("ml-auto", column.typeClassName)}>{column.type}</span>
                </div>
            ))}
        </div>
    );
}
