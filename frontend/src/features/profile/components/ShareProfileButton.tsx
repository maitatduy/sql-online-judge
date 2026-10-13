"use client";

import { Check, Share2 } from "lucide-react";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

const STATUS_RESET_DELAY_MS = 2000;

type CopyStatus = "idle" | "copied" | "failed";

const STATUS_MESSAGES: Record<CopyStatus, string> = {
    idle: "",
    copied: "Đã sao chép liên kết hồ sơ",
    failed: "Không sao chép được liên kết",
};

interface ShareProfileButtonProps {
    username: string;
    children?: React.ReactNode;
}

export function ShareProfileButton({ username, children }: ShareProfileButtonProps) {
    const [copyStatus, setCopyStatus] = useState<CopyStatus>("idle");

    useEffect(() => {
        if (copyStatus === "idle") {
            return;
        }
        const resetTimer = setTimeout(() => setCopyStatus("idle"), STATUS_RESET_DELAY_MS);
        return () => clearTimeout(resetTimer);
    }, [copyStatus]);

    async function handleShare() {
        const profileUrl = `${window.location.origin}${ROUTES.profile(username)}`;
        try {
            await navigator.clipboard.writeText(profileUrl);
            setCopyStatus("copied");
        } catch {
            setCopyStatus("failed");
        }
    }

    return (
        <div className="flex flex-col gap-1.5">
            <div className="flex gap-2">
                {children}
                <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    aria-label="Chia sẻ hồ sơ"
                    onClick={() => void handleShare()}
                >
                    {copyStatus === "copied" ? (
                        <Check aria-hidden="true" />
                    ) : (
                        <Share2 aria-hidden="true" />
                    )}
                </Button>
            </div>
            <p role="status" className="text-xs text-muted-foreground empty:hidden">
                {STATUS_MESSAGES[copyStatus]}
            </p>
        </div>
    );
}
