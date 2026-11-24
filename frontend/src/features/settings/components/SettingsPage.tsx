"use client";

import Link from "next/link";

import { EmailPreferenceToggle } from "@/components/shared/EmailPreferenceToggle";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError, isUnauthenticatedError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { useAccountProfile } from "../hooks/useAccountSettings";

import { AccountProfileForm } from "./AccountProfileForm";
import { AvatarSettings } from "./AvatarSettings";
import { PasswordForm } from "./PasswordForm";

export function SettingsPage() {
    const { data: profile, error, isPending, refetch } = useAccountProfile();

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex max-w-3xl flex-col gap-5 px-4 pt-7 pb-16"
        >
            <div className="flex flex-wrap items-baseline justify-between gap-2">
                <h1 className="text-[24px] font-extrabold">Cài đặt</h1>
                {profile ? (
                    <Link
                        href={ROUTES.profile(profile.username)}
                        className="text-sm text-brand hover:underline"
                    >
                        Xem hồ sơ công khai
                    </Link>
                ) : null}
            </div>
            {isPending ? (
                <div
                    role="status"
                    aria-label="Đang tải cài đặt"
                    aria-busy="true"
                    className="grid gap-5"
                >
                    <Skeleton className="h-28" />
                    <Skeleton className="h-80" />
                </div>
            ) : null}
            {profile ? (
                <>
                    <AvatarSettings profile={profile} />
                    <AccountProfileForm profile={profile} />
                    <section aria-labelledby="notification-settings" className="grid gap-3">
                        <h2 id="notification-settings" className="text-base font-bold">
                            Thông báo
                        </h2>
                        <EmailPreferenceToggle />
                    </section>
                    <PasswordForm hasPassword={profile.hasPassword} />
                    <p className="text-sm text-muted-foreground">
                        Email đăng nhập: {profile.email}
                    </p>
                </>
            ) : null}
            {isUnauthenticatedError(error) ? (
                <EmptyState
                    title="Đăng nhập để chỉnh cài đặt"
                    action={
                        <Button asChild size="sm">
                            <Link href={ROUTES.loginWithNext(ROUTES.settings)}>Đăng nhập</Link>
                        </Button>
                    }
                />
            ) : null}
            {error && !profile && !isUnauthenticatedError(error) ? (
                <ErrorState
                    title="Không tải được cài đặt"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
        </main>
    );
}
