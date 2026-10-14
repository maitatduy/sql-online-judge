import { ImagePlus, UserRound } from "lucide-react";
import Image from "next/image";
import { useId, useState } from "react";

import { Button } from "@/components/ui/button";
import {
    ACCEPTED_AVATAR_TYPES,
    avatarFileError,
    readAsDataUrl,
    type AvatarChange,
} from "@/features/admin/users/avatar-file";

const AVATAR_IMAGE_SIZE = 320;

interface AvatarFieldProps {
    currentUrl: string | null;
    change: AvatarChange;
    onChange: (change: AvatarChange) => void;
}

function shownUrlOf(currentUrl: string | null, change: AvatarChange): string | null {
    if (change.kind === "replace") {
        return change.previewUrl;
    }
    return change.kind === "remove" ? null : currentUrl;
}

export function AvatarField({ currentUrl, change, onChange }: AvatarFieldProps) {
    const inputId = useId();
    const hintId = useId();
    const [fileError, setFileError] = useState<string | null>(null);
    const shownUrl = shownUrlOf(currentUrl, change);

    async function handleFile(file: File | undefined) {
        if (!file) {
            return;
        }
        const invalidFile = avatarFileError(file);
        setFileError(invalidFile);
        if (!invalidFile) {
            onChange({ kind: "replace", file, previewUrl: await readAsDataUrl(file) });
        }
    }

    return (
        <fieldset className="flex items-center gap-4">
            <legend className="sr-only">Ảnh đại diện</legend>
            {shownUrl ? (
                <Image
                    unoptimized
                    width={AVATAR_IMAGE_SIZE}
                    height={AVATAR_IMAGE_SIZE}
                    src={shownUrl}
                    alt="Xem trước ảnh đại diện"
                    className="size-16 shrink-0 rounded-full border object-cover"
                />
            ) : (
                <span
                    aria-hidden="true"
                    className="flex size-16 shrink-0 items-center justify-center rounded-full border border-dashed bg-surface text-muted-foreground"
                >
                    <UserRound className="size-7" />
                </span>
            )}
            <div className="flex min-w-0 flex-col gap-1.5">
                <div className="flex flex-wrap gap-2">
                    <Button asChild size="sm" variant="outline">
                        <label htmlFor={inputId} className="cursor-pointer">
                            <ImagePlus aria-hidden="true" />
                            Chọn ảnh
                        </label>
                    </Button>
                    <input
                        id={inputId}
                        type="file"
                        accept={ACCEPTED_AVATAR_TYPES.join(",")}
                        aria-describedby={hintId}
                        className="sr-only"
                        onChange={(event) => {
                            void handleFile(event.target.files?.[0]);
                            event.target.value = "";
                        }}
                    />
                    {shownUrl ? (
                        <Button
                            type="button"
                            size="sm"
                            variant="ghost"
                            onClick={() =>
                                onChange(currentUrl ? { kind: "remove" } : { kind: "keep" })
                            }
                        >
                            Xoá ảnh
                        </Button>
                    ) : null}
                </div>
                <p id={hintId} className="text-xs text-muted-foreground">
                    PNG, JPG hoặc WEBP, tối đa 2 MB.
                </p>
                {fileError ? <p className="text-[13px] text-destructive">{fileError}</p> : null}
            </div>
        </fieldset>
    );
}
