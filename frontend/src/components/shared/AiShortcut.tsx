"use client";

import { Sparkles } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

export function AiShortcut() {
    const { data: currentUser } = useCurrentUser();

    if (!currentUser) {
        return null;
    }

    return (
        <Button
            asChild
            size="icon"
            variant="ghost"
            title="Trợ lý AI"
            className="size-11 text-ai hover:bg-ai-soft hover:text-ai"
        >
            <Link href={ROUTES.ai} aria-label="Trợ lý AI">
                <Sparkles aria-hidden="true" className="size-4.5" />
            </Link>
        </Button>
    );
}
