"use client";

import { ChevronLeft, Database } from "lucide-react";
import Link from "next/link";
import { notFound } from "next/navigation";

import { ErrorState } from "@/components/shared/ErrorState";
import { Logo } from "@/components/shared/Logo";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ROUTES } from "@/lib/routes";

import { isProblemNotFound, useProblemDetail } from "../hooks/useProblemDetail";
import type { ProblemDetail } from "../types";
import { EditorPanel } from "./EditorPanel";
import { ProblemStatement } from "./ProblemStatement";
import { SchemaPanel } from "./SchemaPanel";

interface WorkspaceProps {
    slug: string;
}

function WorkspaceHeader({ title }: { title: string | null }) {
    return (
        <header className="flex h-12 shrink-0 items-center gap-3 border-b bg-card px-3">
            <Button asChild variant="ghost" size="icon-sm">
                <Link href={ROUTES.problems} aria-label="Quay lại danh sách bài">
                    <ChevronLeft aria-hidden="true" />
                </Link>
            </Button>
            <Link href={ROUTES.home} aria-label="Trang chủ">
                <Logo className="[&>span]:hidden [&>svg]:size-7" />
            </Link>
            {title ? <span className="truncate font-semibold">{title}</span> : null}
        </header>
    );
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

function WorkspaceBody({ problem }: { problem: ProblemDetail }) {
    return (
        <div className="flex min-h-0 flex-1 flex-col lg:flex-row">
            <Tabs
                defaultValue="statement"
                aria-label="Đề bài và schema"
                className="min-h-0 border-b lg:w-[38%] lg:min-w-[420px] lg:border-r lg:border-b-0"
            >
                <TabsList>
                    <TabsTrigger value="statement">Đề bài</TabsTrigger>
                    <TabsTrigger value="schema">
                        <Database aria-hidden="true" />
                        Schema
                    </TabsTrigger>
                </TabsList>
                <TabsContent value="statement" className="overflow-y-auto">
                    <ProblemStatement problem={problem} />
                </TabsContent>
                <TabsContent value="schema" className="overflow-y-auto">
                    <SchemaPanel tables={problem.schema} />
                </TabsContent>
            </Tabs>
            <div className="flex min-h-[420px] min-w-0 flex-1 flex-col">
                <EditorPanel problem={problem} />
            </div>
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
            <WorkspaceHeader title={problem ? `#${problem.number} · ${problem.title}` : null} />
            {problem ? <WorkspaceBody problem={problem} /> : null}
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
