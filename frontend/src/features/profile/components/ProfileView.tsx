"use client";

import { notFound } from "next/navigation";

import { ErrorState } from "@/components/shared/ErrorState";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";

import { useProfile, useRatingSummary, useSolvedSummary } from "../hooks/useProfile";

import { ActivityHeatmapCard } from "./ActivityHeatmapCard";
import { ProfileActions } from "./ProfileActions";
import { ProfileIdentityCard } from "./ProfileIdentityCard";
import { ProfileSkeleton } from "./ProfileSkeleton";
import { RatingHistoryCard } from "./RatingHistoryCard";
import { ProfileStatTiles } from "./ProfileStatTiles";
import { RecentSubmissionsTable } from "./RecentSubmissionsTable";
import { SolvedDonut } from "./SolvedDonut";

const USER_NOT_FOUND_CODE = "USER_NOT_FOUND";

interface ProfileViewProps {
    username: string;
}

function isUserNotFound(error: unknown): boolean {
    return error instanceof ApiRequestError && error.code === USER_NOT_FOUND_CODE;
}

export function ProfileView({ username }: ProfileViewProps) {
    const { data: profile, error, isPending, refetch } = useProfile(username);
    const { data: rating } = useRatingSummary(profile?.userId);
    const { data: solved } = useSolvedSummary(profile?.userId);

    if (isUserNotFound(error)) {
        notFound();
    }

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex max-w-7xl flex-wrap items-start gap-6 px-4 pt-7 pb-16 md:px-6"
        >
            {isPending ? <ProfileSkeleton /> : null}
            {profile ? (
                <>
                    <ProfileIdentityCard
                        profile={profile}
                        rating={rating}
                        actions={<ProfileActions username={profile.username} />}
                    />
                    <div className="flex min-w-0 flex-[999_1_600px] flex-col gap-5">
                        <ProfileStatTiles rating={rating} solved={solved} />
                        <div className="flex flex-wrap gap-5">
                            {solved ? <SolvedDonut solved={solved} /> : null}
                            <RatingHistoryCard
                                userId={profile.userId}
                                username={profile.username}
                            />
                        </div>
                        <ActivityHeatmapCard userId={profile.userId} />
                        <section
                            aria-labelledby="recent-submissions"
                            className="min-w-0 overflow-hidden rounded-lg border bg-card"
                        >
                            <h2 id="recent-submissions" className="border-b px-5 py-3 font-bold">
                                Bài nộp gần đây
                            </h2>
                            <RecentSubmissionsTable
                                userId={profile.userId}
                                username={profile.username}
                            />
                        </section>
                    </div>
                </>
            ) : null}
            {error && !profile ? (
                <ErrorState
                    title="Không tải được hồ sơ"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="w-full"
                />
            ) : null}
        </main>
    );
}
