"use client";

import { notFound } from "next/navigation";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ToastProvider } from "@/components/shared/ToastProvider";

import { isProblemNotFound, useProblemDetail } from "../hooks/useProblemDetail";
import { ProblemWorkspace } from "./ProblemWorkspace";
import { WorkspaceHeader } from "./WorkspaceHeader";

interface WorkspaceProps {
    slug: string;
}

function WorkspaceSkeleton() {
    return (
        <div role="status" aria-label="Đang tải đề bài" className="flex flex-col gap-3 p-6">
            <Skeleton className="h-6 w-1/3" />
            <Skeleton className="h-4 w-1/4" />
            <Skeleton className="mt-3 h-4 w-3/4" />
            <Skeleton className="h-4 w-2/3" />
        </div>
    );
}

export function Workspace({ slug }: WorkspaceProps) {
    const { data: problem, error, isPending, refetch } = useProblemDetail(slug);

    if (isProblemNotFound(error)) {
        notFound();
    }

    return (
        <div className="dark flex h-screen flex-col overflow-hidden bg-background text-foreground">
            {problem ? (
                <ToastProvider>
                    <ProblemWorkspace key={problem.id} problem={problem} />
                </ToastProvider>
            ) : null}
            {!problem ? <WorkspaceHeader title={null} /> : null}
            {isPending ? <WorkspaceSkeleton /> : null}
            {!problem && error ? (
                <ErrorState
                    title="Chưa tải được đề bài"
                    message="Kiểm tra kết nối rồi thử lại."
                    onRetry={() => void refetch()}
                    className="m-6"
                />
            ) : null}
        </div>
    );
}
