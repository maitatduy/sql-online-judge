"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import {
    currentStreakDays,
    mergeActivity,
    shiftDateKey,
    vietnamDateKey,
} from "@/lib/activity-calendar";
import { isUnauthenticatedError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import {
    useRecentSubmissions,
    useSubmissionActivity,
    useSubmissionStats,
    useSuggestedProblems,
} from "../../hooks/useDashboardData";
import { useEnrollments } from "../../hooks/useEnrollments";
import { useLearningActivity } from "../../hooks/useLearningActivity";
import { ActivitySection } from "./ActivitySection";
import { ComingSoonSection } from "./ComingSoonSection";
import { ContinueLearningSection } from "./ContinueLearningSection";
import { DailyChallengeSection } from "./DailyChallengeSection";
import { DashboardHeader } from "./DashboardHeader";
import { DashboardStats } from "./DashboardStats";
import { RecentSubmissionsSection } from "./RecentSubmissionsSection";

const ACTIVITY_DAYS = 364;
const ROW = "flex flex-wrap gap-6";

export function LearningDashboard() {
    const pathname = usePathname();
    const router = useRouter();
    const [now] = useState(() => new Date());
    const todayKey = vietnamDateKey(now);
    const from = shiftDateKey(todayKey, -ACTIVITY_DAYS);
    const { data: currentUser, error: userError } = useCurrentUser();
    const enrollments = useEnrollments();
    const learningActivity = useLearningActivity(from);
    const submissionActivity = useSubmissionActivity(from);
    const stats = useSubmissionStats();
    const recent = useRecentSubmissions();
    const suggested = useSuggestedProblems();
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
    const challenge = suggested.data?.items[0];

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 pt-8 pb-16 text-base md:px-6"
        >
            <DashboardHeader
                displayName={currentUser?.displayName}
                streakDays={activity ? currentStreakDays(activity, todayKey) : undefined}
                challengeHref={challenge ? ROUTES.problem(challenge.slug) : ROUTES.problems}
                now={now}
            />
            <DashboardStats stats={stats.data} />
            <div className={ROW}>
                <ContinueLearningSection enrollments={enrollments.data} />
                <DailyChallengeSection problems={suggested.data?.items} />
            </div>
            <div className={ROW}>
                <ActivitySection days={activity} now={now} />
                <ComingSoonSection
                    title="Kỹ năng theo chủ đề"
                    note="Mở khi đủ bài đã giải theo từng chủ đề"
                />
            </div>
            <div className={ROW}>
                <RecentSubmissionsSection submissions={recent.data} now={now} />
                <ComingSoonSection title="Contest sắp tới" note="Mở cùng tính năng contest" />
            </div>
        </main>
    );
}
