"use client";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Pagination } from "@/components/shared/Pagination";
import type { PageInfo } from "@/lib/pagination";

import { PAGE_SIZES } from "../list-options";

interface ListFooterProps {
    result: PageInfo;
    hrefOf: (page: number) => string;
    unit: string;
    size: number;
    onSizeChange: (size: number) => void;
}

export function ListFooter({ result, hrefOf, unit, size, onSizeChange }: ListFooterProps) {
    return (
        <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg border bg-card">
            <div className="min-w-0 flex-1">
                <Pagination result={result} hrefOf={hrefOf} unit={unit} />
            </div>
            <label className="flex items-center gap-2 px-3.5 py-2.5 text-[13px] text-muted-foreground">
                Số dòng mỗi trang
                <NativeSelect
                    aria-label="Số dòng mỗi trang"
                    value={size}
                    onChange={(event) => onSizeChange(Number(event.target.value))}
                    className="h-8"
                >
                    {PAGE_SIZES.map((pageSize) => (
                        <option key={pageSize} value={pageSize}>
                            {pageSize}
                        </option>
                    ))}
                </NativeSelect>
            </label>
        </div>
    );
}
