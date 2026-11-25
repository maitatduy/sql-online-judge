"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { mergeActivity } from "@/lib/activity-calendar";
import { isUnauthenticatedError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { activitySince, useLearningStreak } from "@/lib/useLearningStreak";

import {
    useDailyChallenges,
    useRatingSummary,
    useRecentSubmissions,
    useSubmissionActivity,
    useSubmissionStats,
    useTopicProgress,
    useUpcomingContests,
} from "../../hooks/useDashboardData";
import { useEnrollments } from "../../hooks/useEnrollments";
import { useLearningActivity } from "../../hooks/useLearningActivity";
import { ActivitySection } from "./ActivitySection";
import { ContinueLearningSection } from "./ContinueLearningSection";
import { DailyChallengeSection } from "./DailyChallengeSection";
import { DashboardHeader } from "./DashboardHeader";
import { DashboardStats } from "./DashboardStats";
import { RecentSubmissionsSection } from "./RecentSubmissionsSection";
import { TopicSkillsSection } from "./TopicSkillsSection";
import { UpcomingContestsSection } from "./UpcomingContestsSection";

const ROW = "flex flex-wrap gap-6";

export function LearningDashboard() {
    const pathname = usePathname();
    const router = useRouter();
    const [now] = useState(() => new Date());
    const from = activitySince(now);
    const streakDays = useLearningStreak(true);
    const { data: currentUser, error: userError } = useCurrentUser();
    const enrollments = useEnrollments();
    const learningActivity = useLearningActivity(from);
    const submissionActivity = useSubmissionActivity(from);
    const stats = useSubmissionStats();
    const recent = useRecentSubmissions();
    const dailyChallenges = useDailyChallenges();
    const topicProgress = useTopicProgress(currentUser?.id);
    const rating = useRatingSummary(currentUser?.id);
    const upcomingContests = useUpcomingContests();
    const isUnauthenticated = isUnauthenticatedError(userError);

    useEffect(() => {
        if (isUnauthenticated) {
            router.replace(ROUTES.loginWithNext(pathname));
        }
    }, [isUnauthenticated, pathname, router]);

    const activity =
        learningActivity.data && submissionActivity.data
            ? mergeActivity(learningActivity.data, submissionActivity.data)
            : undefined;
    const challenge = dailyChallenges.data?.items[0];

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 pt-8 pb-16 text-base md:px-6"
        >
            <DashboardHeader
                displayName={currentUser?.displayName}
                streakDays={streakDays}
                challengeHref={challenge ? ROUTES.problem(challenge.slug) : ROUTES.problems}
                now={now}
            />
            <DashboardStats stats={stats.data} rating={rating.data} />
            <div className={ROW}>
                <ContinueLearningSection enrollments={enrollments.data} />
                <DailyChallengeSection problems={dailyChallenges.data?.items} />
            </div>
            <div className={ROW}>
                <ActivitySection days={activity} now={now} />
                <TopicSkillsSection topics={topicProgress.data?.topics} />
            </div>
            <div className={ROW}>
                <RecentSubmissionsSection submissions={recent.data} now={now} />
                <UpcomingContestsSection contests={upcomingContests.data?.items} />
            </div>
        </main>
    );
}
