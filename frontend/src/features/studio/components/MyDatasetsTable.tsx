"use client";

import { useState } from "react";

import Link from "next/link";

import { formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { StudioDatasetSummary } from "../types";

const CELL_CLASS = "border-b px-4 py-3 align-middle";
const HEADER_CLASS = "border-b px-4 py-2.5 text-left text-xs font-semibold text-muted-foreground";

interface MyDatasetsTableProps {
    datasets: StudioDatasetSummary[];
}

function versionLabel({ latestVersionNumber, hasDraft }: StudioDatasetSummary): string {
    return hasDraft ? `v${latestVersionNumber} · Nháp` : `v${latestVersionNumber}`;
}

export function MyDatasetsTable({ datasets }: MyDatasetsTableProps) {
    const [now] = useState(() => new Date());

    return (
        <div className="overflow-x-auto rounded-lg border bg-card">
            <table
                aria-label="Dataset của tôi"
                className="w-full min-w-[640px] border-collapse text-sm"
            >
                <thead>
                    <tr>
                        <th scope="col" className={HEADER_CLASS}>
                            Tên
                        </th>
                        <th scope="col" className={HEADER_CLASS}>
                            Mô tả
                        </th>
                        <th scope="col" className={cn(HEADER_CLASS, "w-32")}>
                            Phiên bản
                        </th>
                        <th scope="col" className={cn(HEADER_CLASS, "w-36")}>
                            Cập nhật
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {datasets.map((dataset) => (
                        <tr key={dataset.id}>
                            <td className={cn(CELL_CLASS, "font-semibold")}>
                                <Link
                                    href={ROUTES.studioDataset(dataset.id)}
                                    className="hover:text-brand hover:underline"
                                >
                                    {dataset.name}
                                </Link>
                            </td>
                            <td className={cn(CELL_CLASS, "text-[13px] text-muted-foreground")}>
                                {dataset.description}
                            </td>
                            <td className={CELL_CLASS}>
                                <span className="rounded-full bg-surface-2 px-2.5 py-0.5 font-mono text-xs font-semibold">
                                    {versionLabel(dataset)}
                                </span>
                            </td>
                            <td className={cn(CELL_CLASS, "text-[13px] text-muted-foreground")}>
                                <time dateTime={dataset.updatedAt}>
                                    {formatRelativeTime(dataset.updatedAt, now)}
                                </time>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
