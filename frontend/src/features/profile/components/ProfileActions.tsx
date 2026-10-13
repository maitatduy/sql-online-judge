"use client";

import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import { useIsProfileOwner } from "../hooks/useIsProfileOwner";

import { ShareProfileButton } from "./ShareProfileButton";

interface ProfileActionsProps {
    username: string;
}

export function ProfileActions({ username }: ProfileActionsProps) {
    const isOwner = useIsProfileOwner(username);

    return (
        <ShareProfileButton username={username}>
            {isOwner ? (
                <Button asChild variant="outline" className="flex-1">
                    <Link href={ROUTES.settings}>Sửa hồ sơ</Link>
                </Button>
            ) : null}
        </ShareProfileButton>
    );
}
