"use client";

import { MailCheck } from "lucide-react";
import Link from "next/link";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatDate } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useAcceptInvitation } from "../hooks/useAcceptInvitation";
import { useRoleInvitation } from "../hooks/useRoleInvitation";
import type { InvitationStatus, InvitedRole, RoleInvitationView } from "../types";

const ROLE_LABELS: Record<InvitedRole, string> = {
    SETTER: "Người ra đề",
    MODERATOR: "Kiểm duyệt",
};

const CLOSED_TITLES: Record<Exclude<InvitationStatus, "PENDING">, string> = {
    EXPIRED: "Lời mời đã hết hạn",
    REVOKED: "Lời mời đã bị huỷ",
    ACCEPTED: "Lời mời đã được nhận",
};

const CLOSED_HINT = "Nếu vẫn cần quyền này, hãy nhờ quản trị viên gửi lời mời mới.";

interface InvitationPanelProps {
    token: string;
}

export function InvitationPanel({ token }: InvitationPanelProps) {
    const { data: invitation, error, isPending, refetch } = useRoleInvitation(token);

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-4 py-10"
        >
            <section className="flex flex-col gap-4 rounded-lg border bg-card p-6">
                {isPending ? (
                    <div
                        role="status"
                        aria-label="Đang tải lời mời"
                        className="flex flex-col gap-3"
                    >
                        <Skeleton className="h-6 w-3/4" />
                        <Skeleton className="h-4" />
                        <Skeleton className="h-9 w-40" />
                    </div>
                ) : null}
                {error ? <InvitationError error={error} onRetry={() => void refetch()} /> : null}
                {invitation ? <InvitationBody token={token} invitation={invitation} /> : null}
            </section>
        </main>
    );
}

function InvitationError({ error, onRetry }: { error: Error; onRetry: () => void }) {
    if (error instanceof ApiRequestError && error.status === 404) {
        return (
            <Message
                title="Không tìm thấy lời mời"
                text="Link có thể bị sai, hoặc quản trị viên đã gửi một link mới thay thế."
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
    const roleLabel = ROLE_LABELS[invitation.role];

    if (acceptInvitation.isSuccess) {
        return (
            <>
                <Message title={`Bạn đã là ${roleLabel}`} text="Quyền mới đã có hiệu lực." />
                <Button asChild className="self-start">
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
            <MailCheck aria-hidden="true" className="size-8 text-brand" />
            <div>
                <h1 className="text-xl font-extrabold">
                    {invitation.inviterUsername ?? "Quản trị viên"} mời bạn nhận vai trò {roleLabel}
                </h1>
                <p className="mt-1.5 text-sm text-muted-foreground">
                    Lời mời gửi tới {invitation.maskedEmail} · hết hạn ngày{" "}
                    {formatDate(invitation.expiresAt)}
                </p>
            </div>
            {acceptInvitation.error ? (
                <p role="alert" className="text-sm text-destructive">
                    {acceptErrorText(acceptInvitation.error, invitation.maskedEmail)}
                </p>
            ) : null}
            {isUserPending ? null : currentUser ? (
                <Button
                    type="button"
                    className="self-start"
                    disabled={acceptInvitation.isPending}
                    onClick={() => acceptInvitation.mutate()}
                >
                    Nhận quyền
                </Button>
            ) : (
                <div className="flex flex-wrap gap-2">
                    <Button asChild>
                        <Link href={ROUTES.loginWithNext(nextPath)}>Đăng nhập để nhận quyền</Link>
                    </Button>
                    <Button asChild variant="outline">
                        <Link href={ROUTES.registerWithNext(nextPath)}>Đăng ký tài khoản</Link>
                    </Button>
                </div>
            )}
        </>
    );
}

function acceptErrorText(error: Error, maskedEmail: string): string {
    if (error instanceof ApiRequestError && error.code === "INVITATION_EMAIL_MISMATCH") {
        return `Lời mời này gửi tới ${maskedEmail}. Hãy đăng nhập đúng tài khoản đó để nhận quyền.`;
    }
    return error.message;
}

function Message({ title, text }: { title: string; text: string }) {
    return (
        <div>
            <h1 className="text-xl font-extrabold">{title}</h1>
            <p className="mt-1.5 text-sm text-muted-foreground">{text}</p>
        </div>
    );
}
