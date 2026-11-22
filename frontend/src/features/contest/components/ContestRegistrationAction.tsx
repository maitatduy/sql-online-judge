"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { CircleCheck } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { isUnauthenticatedError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useChangeRegistration, useContestRegistration } from "../hooks/useContests";
import type { ContestDetail } from "../types";

interface ContestRegistrationActionProps {
    contest: ContestDetail;
}

export function ContestRegistrationAction({ contest }: ContestRegistrationActionProps) {
    const pathname = usePathname();
    const showToast = useToast();
    const { data: currentUser, error: userError } = useCurrentUser();
    const { data: registration } = useContestRegistration(contest.slug, currentUser !== undefined);
    const change = useChangeRegistration(contest.slug);

    if (contest.phase === "FINISHED") {
        return null;
    }
    if (!currentUser) {
        return isUnauthenticatedError(userError) ? (
            <Button asChild size="lg" className="self-start">
                <Link href={ROUTES.loginWithNext(pathname)}>Đăng nhập để đăng ký</Link>
            </Button>
        ) : null;
    }
    if (!registration) {
        return null;
    }

    function changeRegistration(isRegistering: boolean) {
        change.mutate(isRegistering, {
            onError: (error) => showToast(error.message, "error"),
        });
    }

    if (!registration.isRegistered) {
        return (
            <Button
                type="button"
                size="lg"
                className="self-start"
                disabled={change.isPending}
                onClick={() => changeRegistration(true)}
            >
                Đăng ký
            </Button>
        );
    }
    return (
        <div className="flex flex-wrap items-center gap-3">
            <span className="inline-flex items-center gap-1.5 text-lg font-semibold text-contest-hero-accent">
                <CircleCheck aria-hidden="true" className="size-5" />
                Bạn đã đăng ký
            </span>
            {contest.phase === "UPCOMING" ? (
                <Button
                    type="button"
                    variant="outline"
                    className="border-contest-hero-line bg-transparent text-contest-hero-text hover:bg-contest-hero-panel hover:text-contest-hero-text"
                    disabled={change.isPending}
                    onClick={() => changeRegistration(false)}
                >
                    Huỷ đăng ký
                </Button>
            ) : null}
        </div>
    );
}
