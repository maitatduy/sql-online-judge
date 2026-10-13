import { Avatar } from "@/components/shared/Avatar";
import { formatNumber } from "@/lib/format";

import type { ProfileResponse } from "../types";

import { BadgeGrid } from "./BadgeGrid";
import { ProfileMetaList } from "./ProfileMetaList";

interface ProfileIdentityCardProps {
    profile: ProfileResponse;
    actions?: React.ReactNode;
}

export function ProfileIdentityCard({ profile, actions }: ProfileIdentityCardProps) {
    return (
        <aside className="flex min-w-0 flex-[1_1_280px] flex-col gap-4 rounded-lg border bg-card p-6 lg:max-w-[340px]">
            <div className="flex flex-col items-start gap-3">
                <Avatar name={profile.displayName} className="size-22 text-3xl" />
                <div className="min-w-0">
                    <h1 className="text-[22px] font-extrabold break-words">
                        {profile.displayName}
                    </h1>
                    <p className="mt-0.5 break-all text-muted-foreground">@{profile.username}</p>
                </div>
                <span className="inline-flex h-6 items-center rounded-full bg-brand-soft px-2.5 text-xs font-semibold text-brand">
                    {profile.rankTitle} · {formatNumber(profile.rating)}
                </span>
            </div>
            {profile.bio ? (
                <p className="leading-[1.6] break-words whitespace-pre-line">{profile.bio}</p>
            ) : null}
            <ProfileMetaList
                school={profile.school}
                location={profile.location}
                websiteUrl={profile.websiteUrl}
            />
            <dl className="flex flex-wrap gap-x-5 gap-y-1 text-[13px] whitespace-nowrap">
                <div className="flex flex-row-reverse gap-1">
                    <dt className="text-muted-foreground">người theo dõi</dt>
                    <dd className="font-bold">{formatNumber(profile.followerCount)}</dd>
                </div>
                <div className="flex flex-row-reverse gap-1">
                    <dt className="text-muted-foreground">đang theo dõi</dt>
                    <dd className="font-bold">{formatNumber(profile.followingCount)}</dd>
                </div>
            </dl>
            {actions}
            <BadgeGrid badges={profile.badges} />
        </aside>
    );
}
