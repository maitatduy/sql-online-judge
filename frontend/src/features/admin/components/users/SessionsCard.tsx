import dynamic from "next/dynamic";
import { useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { useRevokeSession, useUserSessions } from "@/features/admin/hooks/useUserSessions";
import type { UserSession } from "@/features/admin/users/types";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { BlockEmpty, BlockError, BlockLoading } from "./BlockStates";
import { PageCard } from "./PageCard";
import { SessionList, sessionLabel } from "./SessionList";

const RevokeAllSessionsDialog = dynamic(
    () =>
        import("./RevokeAllSessionsDialog").then(
            (dialogModule) => dialogModule.RevokeAllSessionsDialog,
        ),
    { ssr: false },
);

interface SessionsCardProps {
    username: string;
    isReadOnly: boolean;
    limit?: number;
    onShowAll?: () => void;
}

export function SessionsCard({ username, isReadOnly, limit, onShowAll }: SessionsCardProps) {
    const showToast = useToast();
    const { data: currentUser } = useCurrentUser();
    const { data: sessions, error, isPending, refetch } = useUserSessions(username);
    const revokeSession = useRevokeSession(username);
    const [isConfirmOpen, setIsConfirmOpen] = useState(false);
    const shownSessions = limit === undefined ? sessions : sessions?.slice(0, limit);
    const canRevokeEach = !isReadOnly && limit === undefined;

    function handleRevoke(session: UserSession) {
        revokeSession.mutate(session.id, {
            onSuccess: () => showToast(`Đã đăng xuất phiên ${sessionLabel(session)}.`),
            onError: (failure) =>
                showToast(`Chưa đăng xuất được phiên. ${failure.message}`, "error"),
        });
    }

    return (
        <PageCard
            title="Phiên đăng nhập"
            action={
                onShowAll ? (
                    <button
                        type="button"
                        onClick={onShowAll}
                        className="text-[13px] font-semibold text-brand hover:underline"
                    >
                        Xem tất cả
                    </button>
                ) : null
            }
        >
            {isPending ? <BlockLoading label="Đang tải phiên đăng nhập" /> : null}
            {error && !sessions ? (
                <BlockError error={error} onRetry={() => void refetch()} />
            ) : null}
            {shownSessions && shownSessions.length > 0 ? (
                <SessionList
                    sessions={shownSessions}
                    onRevoke={canRevokeEach ? handleRevoke : undefined}
                />
            ) : null}
            {sessions && sessions.length === 0 ? (
                <BlockEmpty>Chưa có phiên đăng nhập nào.</BlockEmpty>
            ) : null}
            {!isReadOnly && sessions && sessions.length > 0 ? (
                <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    className="mt-3 w-full"
                    onClick={() => setIsConfirmOpen(true)}
                >
                    Đăng xuất mọi thiết bị
                </Button>
            ) : null}
            {isConfirmOpen ? (
                <RevokeAllSessionsDialog
                    username={username}
                    isSelf={currentUser?.username === username}
                    onClose={() => setIsConfirmOpen(false)}
                />
            ) : null}
        </PageCard>
    );
}
