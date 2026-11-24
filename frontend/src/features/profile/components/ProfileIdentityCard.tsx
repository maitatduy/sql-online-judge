import Image from "next/image";

import { Avatar } from "@/components/shared/Avatar";
import { formatNumber } from "@/lib/format";

import type { PublicProfile, RatingSummary } from "../types";

import { ProfileMetaList } from "./ProfileMetaList";

const AVATAR_SIZE = 88;

interface ProfileIdentityCardProps {
    profile: PublicProfile;
    rating: RatingSummary | undefined;
    actions?: React.ReactNode;
}

export function ProfileIdentityCard({ profile, rating, actions }: ProfileIdentityCardProps) {
    const name = profile.displayName ?? profile.username;

    return (
        <aside className="flex min-w-0 flex-[1_1_280px] flex-col gap-4 rounded-lg border bg-card p-6 lg:max-w-[340px]">
            <div className="flex flex-col items-start gap-3">
                {profile.avatarUrl ? (
                    <Image
                        unoptimized
                        width={AVATAR_SIZE}
                        height={AVATAR_SIZE}
                        src={profile.avatarUrl}
                        alt=""
                        className="size-22 rounded-full object-cover"
                    />
                ) : (
                    <Avatar name={name} className="size-22 text-3xl" />
                )}
                <div className="min-w-0">
                    <h1 className="text-[24px] font-extrabold break-words">{name}</h1>
                    <p className="mt-0.5 break-all text-muted-foreground">@{profile.username}</p>
                </div>
                {rating?.rating !== null && rating?.rating !== undefined ? (
                    <span className="inline-flex h-7 items-center rounded-full bg-brand-soft px-3 text-sm font-semibold text-brand">
                        {`Rating ${formatNumber(rating.rating)}`}
                    </span>
                ) : null}
            </div>
            {profile.bio ? (
                <p className="text-[15px] leading-[1.6] break-words whitespace-pre-line">
                    {profile.bio}
                </p>
            ) : null}
            <ProfileMetaList
                school={profile.school}
                location={profile.location}
                websiteUrl={profile.websiteUrl}
            />
            {actions}
        </aside>
    );
}
