"use client";

import { createColumnHelper, tableFeatures, useTable } from "@tanstack/react-table";
import Link from "next/link";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { formatNumber, formatPercent } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { ProblemSummary } from "../types";
import { ProblemProgressMark } from "./ProblemProgressMark";

const features = tableFeatures({});
const column = createColumnHelper<typeof features, ProblemSummary>();

const PROGRESS_COLUMN = column.accessor("progress", {
    header: "Tiến độ",
    cell: (info) => {
        const progress = info.getValue();
        return progress ? <ProblemProgressMark progress={progress} /> : null;
    },
});

const PROBLEM_COLUMNS = [
    column.accessor("number", { header: "#" }),
    column.accessor("title", {
        header: "Tên bài",
        cell: (info) => (
            <Link
                href={ROUTES.problem(info.row.original.slug)}
                className="font-semibold text-foreground hover:text-brand"
            >
                {info.getValue()}
            </Link>
        ),
    }),
    column.accessor("topics", {
        header: "Chủ đề",
        cell: (info) => (
            <span className="flex flex-wrap gap-1">
                {info.getValue().map((topic) => (
                    <span
                        key={topic.slug}
                        className="rounded-sm bg-surface-2 px-1.5 py-0.5 text-xs text-muted-foreground"
                    >
                        {topic.name}
                    </span>
                ))}
            </span>
        ),
    }),
    column.accessor("difficulty", {
        header: "Độ khó",
        cell: (info) => <DifficultyLabel difficulty={info.getValue()} className="text-[13px]" />,
    }),
    column.accessor("acceptanceRate", {
        header: "Tỉ lệ AC",
        cell: (info) => formatPercent(info.getValue()),
    }),
    column.accessor("solvedCount", {
        header: "Người giải",
        cell: (info) => formatNumber(info.getValue()),
    }),
];

const COLUMNS_WITHOUT_PROGRESS = column.columns(PROBLEM_COLUMNS);
const COLUMNS_WITH_PROGRESS = column.columns([PROGRESS_COLUMN, ...PROBLEM_COLUMNS]);

const COLUMN_CLASSES: Record<string, string> = {
    progress: "w-16",
    number: "w-14 font-mono text-muted-foreground",
    topics: "max-md:hidden",
    acceptanceRate: "text-right font-mono",
    solvedCount: "text-right font-mono text-muted-foreground max-md:hidden",
};

interface ProblemTableProps {
    problems: ProblemSummary[];
    showProgress: boolean;
}

export function ProblemTable({ problems, showProgress }: ProblemTableProps) {
    const table = useTable({
        features,
        columns: showProgress ? COLUMNS_WITH_PROGRESS : COLUMNS_WITHOUT_PROGRESS,
        data: problems,
        getRowId: (problem) => problem.id,
    });

    return (
        <div className="overflow-x-auto">
            <table aria-label="Danh sách bài tập" className="w-full border-collapse text-sm">
                <thead>
                    {table.getHeaderGroups().map((headerGroup) => (
                        <tr key={headerGroup.id}>
                            {headerGroup.headers.map((header) => (
                                <th
                                    key={header.id}
                                    scope="col"
                                    className={cn(
                                        "border-b bg-surface px-3 py-2.5 text-left text-xs font-semibold whitespace-nowrap text-muted-foreground",
                                        COLUMN_CLASSES[header.column.id],
                                    )}
                                >
                                    <table.FlexRender header={header} />
                                </th>
                            ))}
                        </tr>
                    ))}
                </thead>
                <tbody>
                    {table.getRowModel().rows.map((row) => (
                        <tr key={row.id} className="hover:bg-surface">
                            {row.getAllCells().map((cell) => (
                                <td
                                    key={cell.id}
                                    className={cn(
                                        "h-11 border-b px-3 py-2 align-middle",
                                        COLUMN_CLASSES[cell.column.id],
                                    )}
                                >
                                    <table.FlexRender cell={cell} />
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
