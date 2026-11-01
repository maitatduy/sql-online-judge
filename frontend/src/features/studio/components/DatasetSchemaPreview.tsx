import Link from "next/link";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ROUTES } from "@/lib/routes";

import { useStudioDataset } from "../hooks/useStudioDataset";
import type { DatasetColumn } from "../types";

interface DatasetSchemaPreviewProps {
    datasetId: string;
}

function columnNote(column: DatasetColumn): string {
    if (column.isPrimaryKey) {
        return "khoá chính";
    }
    if (column.references) {
        return `tham chiếu ${column.references.table}.${column.references.column}`;
    }
    return column.isNullable ? "cho phép NULL" : "";
}

export function DatasetSchemaPreview({ datasetId }: DatasetSchemaPreviewProps) {
    const { data: dataset, error, isPending, refetch } = useStudioDataset(datasetId);

    return (
        <section
            aria-label="Schema của database"
            className="flex flex-col gap-3 rounded-md border bg-surface-2/40 p-4"
        >
            {isPending ? (
                <div role="status" aria-label="Đang tải schema" className="flex flex-col gap-2">
                    <Skeleton className="h-5 w-40" />
                    <Skeleton className="h-24" />
                </div>
            ) : null}
            {error ? (
                <ErrorState
                    title="Không tải được schema"
                    message={error.message}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {dataset ? (
                <>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                        <p className="text-sm font-bold">{dataset.name}</p>
                        <Link
                            href={`${ROUTES.studioDatasets}/${dataset.id}`}
                            aria-label={`Mở database ${dataset.name}`}
                            className="text-[13px] font-semibold text-brand hover:underline"
                        >
                            Mở database
                        </Link>
                    </div>
                    <ul className="grid gap-3 sm:grid-cols-2">
                        {dataset.schema.tables.map((table) => (
                            <li key={table.name} className="rounded-md border bg-card p-3">
                                <p className="text-sm font-semibold">{table.name}</p>
                                <ul className="mt-2 flex flex-col gap-1 text-[13px]">
                                    {table.columns.map((column) => (
                                        <li key={column.name} className="flex flex-wrap gap-x-2">
                                            <span>{column.name}</span>
                                            <span className="text-muted-foreground">
                                                {column.type}
                                            </span>
                                            <span className="text-muted-foreground">
                                                {columnNote(column)}
                                            </span>
                                        </li>
                                    ))}
                                </ul>
                            </li>
                        ))}
                    </ul>
                </>
            ) : null}
        </section>
    );
}
