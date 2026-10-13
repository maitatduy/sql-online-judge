"use client";

import { notFound } from "next/navigation";
import { Suspense } from "react";

import { ErrorState } from "@/components/shared/ErrorState";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";

import { useProfile } from "../hooks/useProfile";

import { ActivityHeatmapCard } from "./ActivityHeatmapCard";
import { ProfileActions } from "./ProfileActions";
import { ProfileIdentityCard } from "./ProfileIdentityCard";
import { ProfileSkeleton } from "./ProfileSkeleton";
import { RatingHistoryCard } from "./RatingHistoryCard";
import { ProfileStatTiles } from "./ProfileStatTiles";
import { ProfileTabs } from "./ProfileTabs";
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
                        actions={<ProfileActions username={profile.username} />}
                    />
                    <div className="flex min-w-0 flex-[999_1_600px] flex-col gap-5">
                        <ProfileStatTiles rating={profile.rating} stats={profile.stats} />
                        <div className="flex flex-wrap gap-5">
                            <SolvedDonut stats={profile.stats} />
                            <RatingHistoryCard username={profile.username} />
                        </div>
                        <ActivityHeatmapCard
                            username={profile.username}
                            longestStreakDays={profile.stats.longestStreakDays}
                        />
                        <Suspense fallback={null}>
                            <ProfileTabs
                                username={profile.username}
                                tabCounts={profile.tabCounts}
                            />
                        </Suspense>
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
