"use client";

import { LockKeyhole } from "lucide-react";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useContestProblems } from "../hooks/useContests";

const HEADER_CLASS = "border-b px-4 py-2.5 text-left text-sm font-semibold text-muted-foreground";
const NOT_REGISTERED = "CONTEST_NOT_REGISTERED";

interface ContestProblemTableProps {
    slug: string;
}

function LockedProblems({ message }: { message: string }) {
    return (
        <section className="flex items-center gap-4 rounded-lg border border-dashed bg-card p-5">
            <LockKeyhole aria-hidden="true" className="size-7 text-muted-foreground" />
            <p className="font-bold">{message}</p>
        </section>
    );
}

export function ContestProblemTable({ slug }: ContestProblemTableProps) {
    const { data: currentUser } = useCurrentUser();
    const {
        data: problems,
        error,
        isPending,
        refetch,
    } = useContestProblems(slug, currentUser !== undefined);

    if (!currentUser) {
        return <LockedProblems message="Đăng nhập để xem đề." />;
    }
    if (error instanceof ApiRequestError && error.code === NOT_REGISTERED) {
        return <LockedProblems message="Đăng ký để xem đề." />;
    }
    if (error) {
        return (
            <ErrorState
                title="Chưa tải được đề"
                message={error.message}
                traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                onRetry={() => void refetch()}
            />
        );
    }
    if (isPending) {
        return (
            <div role="status" aria-label="Đang tải đề">
                <Skeleton className="h-40 rounded-lg" />
            </div>
        );
    }
    return (
        <div className="overflow-x-auto rounded-lg border bg-card">
            <table aria-label="Bài trong contest" className="w-full border-collapse">
                <thead>
                    <tr>
                        <th scope="col" className={`${HEADER_CLASS} w-16`}>
                            Bài
                        </th>
                        <th scope="col" className={HEADER_CLASS}>
                            Tên
                        </th>
                        <th scope="col" className={HEADER_CLASS}>
                            Độ khó
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {problems.map((problem) => (
                        <tr key={problem.letter}>
                            <td className="border-b px-4 py-3 text-lg font-extrabold">
                                {problem.letter}
                            </td>
                            <td className="border-b px-4 py-3 font-semibold">{problem.title}</td>
                            <td className="border-b px-4 py-3">
                                <DifficultyLabel difficulty={problem.difficulty} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
