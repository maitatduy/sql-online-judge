"use client";

import Link from "next/link";
import { notFound } from "next/navigation";

import { LockKeyhole } from "lucide-react";

import type { BreadcrumbItem } from "@/components/shared/Breadcrumb";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ToastProvider } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { practiceApi } from "../api";
import { useContestStatement } from "../hooks/useContestWorkspace";
import type { ContestStatement } from "../types";
import type { WorkspaceTarget } from "../workspace-target";
import { ContestWorkspaceBar } from "./ContestWorkspaceBar";
import { ProblemWorkspace } from "./ProblemWorkspace";
import { WorkspaceHeader } from "./WorkspaceHeader";

const NOT_FOUND = 404;
const FORBIDDEN = 403;

interface ContestWorkspaceProps {
    slug: string;
    letter: string;
}

function contestParentOf(slug: string): BreadcrumbItem {
    return { label: "Contest", href: ROUTES.contest(slug) };
}

function contestTarget(slug: string, statement: ContestStatement): WorkspaceTarget {
    return {
        storageKey: `contest:${slug}:${statement.letter}`,
        heading: `${statement.letter}. ${statement.title}`,
        parent: contestParentOf(slug),
        loginPath: ROUTES.contestProblem(slug, statement.letter),
        historyProblemId: null,
        startRun: (request) => practiceApi.startContestRun(slug, statement.letter, request),
        submit: (request, idempotencyKey) =>
            practiceApi.submitInContest(slug, statement.letter, request, idempotencyKey),
    };
}

function ContestLocked({ slug, message }: { slug: string; message: string }) {
    return (
        <div className="m-6 flex flex-col items-start gap-4 rounded-lg border border-dashed bg-card p-6">
            <LockKeyhole aria-hidden="true" className="size-7 text-muted-foreground" />
            <p className="text-lg font-bold">{message}</p>
            <Button asChild>
                <Link href={ROUTES.contest(slug)}>Về sảnh chờ</Link>
            </Button>
        </div>
    );
}

export function ContestWorkspace({ slug, letter }: ContestWorkspaceProps) {
    const { data: statement, error, isPending, refetch } = useContestStatement(slug, letter);
    const status = error instanceof ApiRequestError ? error.status : null;

    if (status === NOT_FOUND) {
        notFound();
    }

    return (
        <div className="flex h-screen flex-col overflow-hidden bg-background text-foreground">
            {statement ? (
                <ToastProvider>
                    <ProblemWorkspace
                        key={`${slug}:${statement.letter}`}
                        problem={statement}
                        target={contestTarget(slug, statement)}
                        header={<ContestWorkspaceBar slug={slug} letter={statement.letter} />}
                    />
                </ToastProvider>
            ) : null}
            {!statement ? <WorkspaceHeader parent={contestParentOf(slug)} title={null} /> : null}
            {isPending ? (
                <div role="status" aria-label="Đang tải đề bài" className="p-6">
                    <Skeleton className="h-40" />
                </div>
            ) : null}
            {error && status === FORBIDDEN ? (
                <ContestLocked slug={slug} message={error.message} />
            ) : null}
            {error && status !== FORBIDDEN && status !== NOT_FOUND ? (
                <ErrorState
                    title="Chưa tải được đề bài"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="m-6"
                />
            ) : null}
        </div>
    );
}
