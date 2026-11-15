"use client";

import { GraduationCap } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { ActivityHeatmap } from "@/components/shared/ActivityHeatmap";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import {
    buildHeatmapWeeks,
    currentStreakDays,
    shiftDateKey,
    vietnamDateKey,
} from "@/lib/activity-calendar";
import { isUnauthenticatedError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useEnrollments } from "../../hooks/useEnrollments";
import { useLearningActivity } from "../../hooks/useLearningActivity";
import { DashboardStats } from "./DashboardStats";
import { EnrolledCourseCard } from "./EnrolledCourseCard";

const ACTIVITY_DAYS = 364;

export function LearningDashboard() {
    const pathname = usePathname();
    const router = useRouter();
    const [now] = useState(() => new Date());
    const todayKey = vietnamDateKey(now);
    const { data: currentUser, error: userError } = useCurrentUser();
    const enrollments = useEnrollments();
    const activity = useLearningActivity(shiftDateKey(todayKey, -ACTIVITY_DAYS));
    const isUnauthenticated = isUnauthenticatedError(userError);

    useEffect(() => {
        if (isUnauthenticated) {
            router.replace(ROUTES.loginWithNext(pathname));
        }
    }, [isUnauthenticated, pathname, router]);

    const days = activity.data ?? [];
    const lessonCount = days.reduce((sum, day) => sum + day.count, 0);

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-6xl flex-col gap-7 px-4 pt-8 pb-16 text-base md:px-6"
        >
            <h1 className="text-2xl font-extrabold tracking-tight">
                {currentUser ? `Chào ${currentUser.displayName}` : "Chào bạn"}
            </h1>
            {activity.data && enrollments.data ? (
                <DashboardStats
                    streakDays={currentStreakDays(days, todayKey)}
                    lessonCount={lessonCount}
                    courseCount={enrollments.data.length}
                />
            ) : (
                <Skeleton className="h-20 rounded-lg" />
            )}
            <section aria-labelledby="dashboard-courses" className="flex flex-col gap-4">
                <h2 id="dashboard-courses" className="text-lg font-bold">
                    Đang học
                </h2>
                {enrollments.isPending ? <Skeleton className="h-40 rounded-lg" /> : null}
                {enrollments.error && !enrollments.data ? (
                    <ErrorState
                        title="Chưa tải được khoá đang học"
                        onRetry={() => void enrollments.refetch()}
                    />
                ) : null}
                {enrollments.data?.length === 0 ? (
                    <EmptyState
                        icon={<GraduationCap />}
                        title="Chưa học khoá nào"
                        action={
                            <Button asChild size="sm">
                                <Link href={ROUTES.learn}>Xem khoá học</Link>
                            </Button>
                        }
                    />
                ) : null}
                {enrollments.data && enrollments.data.length > 0 ? (
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {enrollments.data.map((course) => (
                            <EnrolledCourseCard key={course.slug} course={course} />
                        ))}
                    </div>
                ) : null}
            </section>
            <section
                aria-labelledby="dashboard-activity"
                className="flex flex-col gap-4 rounded-lg border bg-card p-5"
            >
                <h2 id="dashboard-activity" className="text-lg font-bold">
                    Hoạt động
                </h2>
                {activity.data ? (
                    <ActivityHeatmap
                        weeks={buildHeatmapWeeks(days, now)}
                        total={lessonCount}
                        unit="bài học"
                    />
                ) : (
                    <Skeleton className="h-24" />
                )}
            </section>
        </main>
    );
}
