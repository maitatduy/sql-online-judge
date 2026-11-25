"use client";

import Link from "next/link";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatDate } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useAcceptInvitation } from "../hooks/useAcceptInvitation";
import { useDeclineInvitation } from "../hooks/useDeclineInvitation";
import { useRoleInvitation } from "../hooks/useRoleInvitation";
import type { InvitationStatus, InvitedRole, RoleInvitationView } from "../types";
import { DeclineInvitationButton } from "./DeclineInvitationButton";

const ROLE_LABELS: Record<InvitedRole, string> = {
    SETTER: "Người ra đề",
    MODERATOR: "Kiểm duyệt",
};

const CLOSED_TITLES: Record<Exclude<InvitationStatus, "PENDING">, string> = {
    EXPIRED: "Lời mời đã hết hạn",
    REVOKED: "Lời mời đã bị huỷ",
    ACCEPTED: "Lời mời đã được nhận",
    DECLINED: "Bạn đã từ chối lời mời",
};

const CLOSED_HINT = "Cần quyền này thì nhờ quản trị viên gửi lời mời mới.";
const ACTION_CLASS = "h-12.5 w-full text-[15px]";

interface InvitationPanelProps {
    token: string;
}

export function InvitationPanel({ token }: InvitationPanelProps) {
    const { data: invitation, error, isPending, refetch } = useRoleInvitation(token);

    return (
        <section className="flex flex-col gap-5 rounded-xl border bg-card p-6 shadow-2xl sm:p-8">
            {isPending ? (
                <div
                    role="status"
                    aria-label="Đang tải lời mời"
                    className="flex flex-col items-center gap-3"
                >
                    <Skeleton className="h-7 w-3/4" />
                    <Skeleton className="h-4 w-2/3" />
                    <Skeleton className="h-12 w-full" />
                </div>
            ) : null}
            {error ? <InvitationError error={error} onRetry={() => void refetch()} /> : null}
            {invitation ? <InvitationBody token={token} invitation={invitation} /> : null}
        </section>
    );
}

function InvitationError({ error, onRetry }: { error: Error; onRetry: () => void }) {
    if (error instanceof ApiRequestError && error.status === 404) {
        return (
            <Message
                title="Không tìm thấy lời mời"
                text="Link bị sai, hoặc đã có link mới thay thế."
            />
        );
    }
    return (
        <ErrorState
            title="Chưa tải được lời mời"
            message={error.message}
            traceId={error instanceof ApiRequestError ? error.traceId : undefined}
            onRetry={onRetry}
            className="border-0 p-0"
        />
    );
}

function InvitationBody({ token, invitation }: { token: string; invitation: RoleInvitationView }) {
    const { data: currentUser, isPending: isUserPending } = useCurrentUser();
    const acceptInvitation = useAcceptInvitation(token);
    const declineInvitation = useDeclineInvitation(token);
    const roleLabel = ROLE_LABELS[invitation.role];
    const actionError = acceptInvitation.error ?? declineInvitation.error;
    const isBusy = acceptInvitation.isPending || declineInvitation.isPending;

    if (acceptInvitation.isSuccess) {
        return (
            <>
                <Message title={`Bạn đã là ${roleLabel}`} text="Quyền mới đã có hiệu lực." />
                <Button asChild size="lg" className={ACTION_CLASS}>
                    <Link href={ROUTES.home}>Về trang chủ</Link>
                </Button>
            </>
        );
    }
    if (invitation.status !== "PENDING") {
        return <Message title={CLOSED_TITLES[invitation.status]} text={CLOSED_HINT} />;
    }

    const nextPath = ROUTES.invitation(token);
    return (
        <>
            <Message
                title={`${invitation.inviterUsername ?? "Quản trị viên"} mời bạn làm ${roleLabel}`}
                text={`Gửi tới ${invitation.maskedEmail}, hết hạn ngày ${formatDate(invitation.expiresAt)}.`}
            />
            {actionError ? (
                <p role="alert" className="text-center text-sm text-destructive">
                    {actionErrorText(actionError, invitation.maskedEmail)}
                </p>
            ) : null}
            {isUserPending ? null : (
                <div className="flex flex-col gap-2.5">
                    {currentUser ? (
                        <Button
                            type="button"
                            size="lg"
                            className={ACTION_CLASS}
                            disabled={isBusy}
                            onClick={() => acceptInvitation.mutate()}
                        >
                            Nhận quyền
                        </Button>
                    ) : (
                        <>
                            <Button asChild size="lg" className={ACTION_CLASS}>
                                <Link href={ROUTES.loginWithNext(nextPath)}>
                                    Đăng nhập để nhận quyền
                                </Link>
                            </Button>
                            <Button asChild size="lg" variant="outline" className={ACTION_CLASS}>
                                <Link href={ROUTES.registerWithNext(nextPath)}>
                                    Đăng ký tài khoản
                                </Link>
                            </Button>
                        </>
                    )}
                    <DeclineInvitationButton
                        roleLabel={roleLabel}
                        isPending={isBusy}
                        onConfirm={() => declineInvitation.mutate()}
                    />
                </div>
            )}
        </>
    );
}

function actionErrorText(error: Error, maskedEmail: string): string {
    if (error instanceof ApiRequestError && error.code === "INVITATION_EMAIL_MISMATCH") {
        return `Lời mời này gửi tới ${maskedEmail}. Hãy đăng nhập đúng tài khoản đó.`;
    }
    return error.message;
}

function Message({ title, text }: { title: string; text: string }) {
    return (
        <div className="text-center">
            <h1 className="text-[26px] font-extrabold tracking-[-0.02em]">{title}</h1>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{text}</p>
        </div>
    );
}
