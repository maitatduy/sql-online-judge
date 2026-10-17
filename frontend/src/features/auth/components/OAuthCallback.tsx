"use client";

import { CircleAlert, LoaderCircle } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef } from "react";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";
import { getSafeNextPath } from "@/lib/safe-redirect";

import { getFormErrorMessage } from "../form-errors";
import { useRefreshSession } from "../hooks/useRefreshSession";
import { readOAuthNextPath } from "../oauth-redirect";

import { AuthStatusCard } from "./AuthStatusCard";

export function OAuthCallback() {
    const router = useRouter();
    const refreshSession = useRefreshSession();
    const hasRequested = useRef(false);

    useEffect(() => {
        if (hasRequested.current) {
            return;
        }
        hasRequested.current = true;
        refreshSession.mutate(undefined, {
            onSuccess: () => router.replace(getSafeNextPath(readOAuthNextPath())),
        });
    });

    if (refreshSession.isError) {
        return (
            <AuthStatusCard
                icon={CircleAlert}
                tone="destructive"
                title="Chưa đăng nhập được"
                description={getFormErrorMessage(refreshSession.error)}
            >
                <Button asChild size="lg" className="h-12.5 text-[15px]">
                    <Link href={ROUTES.login}>Quay lại đăng nhập</Link>
                </Button>
            </AuthStatusCard>
        );
    }

    return (
        <AuthStatusCard
            icon={LoaderCircle}
            tone="brand"
            title="Đang đăng nhập"
            isBusy
            description={<span role="status">Đang hoàn tất đăng nhập…</span>}
        />
    );
}
