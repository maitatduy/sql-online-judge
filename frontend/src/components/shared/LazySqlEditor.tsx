"use client";

import dynamic from "next/dynamic";

import { Skeleton } from "./Skeleton";

export const LazySqlEditor = dynamic(
    () => import("./SqlEditor").then((editorModule) => editorModule.SqlEditor),
    {
        ssr: false,
        loading: () => (
            <div role="status" aria-label="Đang mở editor" className="flex flex-col gap-2 p-4">
                <Skeleton className="h-4 w-2/3" />
                <Skeleton className="h-4 w-1/2" />
                <Skeleton className="h-4 w-3/5" />
            </div>
        ),
    },
);
