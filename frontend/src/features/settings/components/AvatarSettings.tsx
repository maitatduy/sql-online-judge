"use client";

import Image from "next/image";
import { useRef } from "react";

import { Avatar } from "@/components/shared/Avatar";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import { useRemoveAvatar, useReplaceAvatar } from "../hooks/useAccountSettings";
import type { AccountProfile } from "../types";

const AVATAR_SIZE = 72;
const MAX_AVATAR_BYTES = 2 * 1024 * 1024;
const ACCEPTED_TYPES = "image/png,image/jpeg,image/webp";

interface AvatarSettingsProps {
    profile: AccountProfile;
}

export function AvatarSettings({ profile }: AvatarSettingsProps) {
    const showToast = useToast();
    const fileInput = useRef<HTMLInputElement>(null);
    const replace = useReplaceAvatar();
    const remove = useRemoveAvatar();
    const isBusy = replace.isPending || remove.isPending;

    const handleFile = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        event.target.value = "";
        if (!file) {
            return;
        }
        if (file.size > MAX_AVATAR_BYTES) {
            showToast("Ảnh tối đa 2 MB", "error");
            return;
        }
        replace.mutate(file, {
            onSuccess: () => showToast("Đã đổi ảnh đại diện"),
            onError: (error) => showToast(error.message, "error"),
        });
    };

    const handleRemove = () =>
        remove.mutate(undefined, {
            onSuccess: () => showToast("Đã gỡ ảnh đại diện"),
            onError: (error) => showToast(error.message, "error"),
        });

    return (
        <section
            aria-labelledby="avatar-settings"
            className="flex flex-wrap items-center gap-4 rounded-lg border bg-card p-6"
        >
            {profile.avatarUrl ? (
                <Image
                    unoptimized
                    width={AVATAR_SIZE}
                    height={AVATAR_SIZE}
                    src={profile.avatarUrl}
                    alt="Ảnh đại diện hiện tại"
                    className="size-18 rounded-full object-cover"
                />
            ) : (
                <Avatar name={profile.displayName} className="size-18 text-2xl" />
            )}
            <div className="min-w-0 flex-1">
                <h2 id="avatar-settings" className="text-base font-bold">
                    Ảnh đại diện
                </h2>
                <p className="text-sm text-muted-foreground">JPG, PNG hoặc WebP, tối đa 2 MB</p>
            </div>
            <div className="flex gap-2">
                <input
                    ref={fileInput}
                    type="file"
                    accept={ACCEPTED_TYPES}
                    aria-label="Chọn ảnh đại diện"
                    className="sr-only"
                    tabIndex={-1}
                    onChange={handleFile}
                />
                <Button type="button" disabled={isBusy} onClick={() => fileInput.current?.click()}>
                    Đổi ảnh
                </Button>
                {profile.avatarUrl ? (
                    <Button
                        type="button"
                        variant="outline"
                        disabled={isBusy}
                        onClick={handleRemove}
                    >
                        Gỡ ảnh
                    </Button>
                ) : null}
            </div>
        </section>
    );
}
