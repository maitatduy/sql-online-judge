import type { CsvCell } from "./csv";
import type { StudioDataset } from "./types";

export interface VariantTableDraft {
    variantId: string;
    tableName: string;
    columns: string[];
    rows: CsvCell[][];
}

export type VariantDrafts = Record<string, VariantTableDraft>;

export function draftKey(variantId: string, tableName: string): string {
    return `${variantId}/${tableName}`;
}

export function withoutDraft(drafts: VariantDrafts, key: string): VariantDrafts {
    return Object.fromEntries(Object.entries(drafts).filter(([candidate]) => candidate !== key));
}

export function columnsOf(dataset: StudioDataset, tableName: string): string[] {
    return (
        dataset.schema.tables
            .find((table) => table.name === tableName)
            ?.columns.map((column) => column.name) ?? []
    );
}

export function liveDrafts(drafts: VariantDrafts, dataset: StudioDataset): VariantTableDraft[] {
    return Object.values(drafts).filter(
        (draft) =>
            dataset.variants.some((variant) => variant.id === draft.variantId) &&
            JSON.stringify(columnsOf(dataset, draft.tableName)) === JSON.stringify(draft.columns),
    );
}
