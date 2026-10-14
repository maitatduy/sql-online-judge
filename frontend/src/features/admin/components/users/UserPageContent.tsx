import { useUserPageTab } from "@/features/admin/hooks/useUserPageTab";
import type { AdminUserDetail } from "@/features/admin/users/types";
import { userMetaLine } from "@/features/admin/users/user-meta";
import { formatDate } from "@/lib/format";

import { AccountStatusLine } from "./AccountStatusLine";
import { ContestHistoryTab } from "./ContestHistoryTab";
import { DetailAvatar } from "./DetailAvatar";
import { OverviewTab } from "./OverviewTab";
import { PageCard } from "./PageCard";
import { SessionsCard } from "./SessionsCard";
import { UserPageActions } from "./UserPageActions";
import { UserPageStats } from "./UserPageStats";
import { UserPageTabs } from "./UserPageTabs";
import { ViolationList } from "./ViolationList";

const CARD_CLASS = "rounded-lg border bg-card p-5";

interface UserPageContentProps {
    user: AdminUserDetail;
}

export function UserPageContent({ user }: UserPageContentProps) {
    const { tab, selectTab } = useUserPageTab();

    return (
        <>
            {user.deletedAt ? (
                <p className="rounded-lg border bg-surface-2 px-4 py-3 text-[13px]">
                    {`Tài khoản đã bị xoá ngày ${formatDate(user.deletedAt)}.`}
                </p>
            ) : null}
            <section className={`${CARD_CLASS} flex flex-wrap items-center gap-4`}>
                <DetailAvatar username={user.username} avatarUrl={user.avatarUrl} />
                <div className="min-w-0 flex-[1_1_260px]">
                    <h1 className="text-2xl font-extrabold break-all">{user.username}</h1>
                    <p className="mb-1.5 text-[13px] text-muted-foreground">{userMetaLine(user)}</p>
                    <AccountStatusLine user={user} />
                </div>
                <UserPageActions user={user} />
            </section>
            <section aria-label="Số liệu">
                <UserPageStats user={user} />
            </section>
            <UserPageTabs
                username={user.username}
                activeTab={tab}
                violationCount={user.violations.length}
                onSelect={selectTab}
            />
            {tab === "overview" ? (
                <OverviewTab
                    user={user}
                    onShowContests={() => selectTab("contests")}
                    onShowSessions={() => selectTab("sessions")}
                />
            ) : null}
            {tab === "contests" ? <ContestHistoryTab username={user.username} /> : null}
            {tab === "sessions" ? (
                <SessionsCard username={user.username} isReadOnly={user.deletedAt !== null} />
            ) : null}
            {tab === "violations" ? (
                <PageCard title="Lịch sử vi phạm">
                    <ViolationList violations={[...user.violations].reverse()} />
                </PageCard>
            ) : null}
        </>
    );
}
