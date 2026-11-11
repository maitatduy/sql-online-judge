"use client";

import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

export function StartLearningActions() {
    const { data: currentUser } = useCurrentUser();

    if (currentUser) {
        return (
            <div className="mt-8 flex flex-wrap gap-3">
                <Button asChild size="lg">
                    <Link href={ROUTES.problems}>Tiếp tục luyện tập</Link>
                </Button>
            </div>
        );
    }

    return (
        <div className="mt-8 flex flex-wrap gap-3">
            <Button asChild size="lg">
                <Link href={ROUTES.register}>Bắt đầu miễn phí</Link>
            </Button>
            <Button asChild size="lg" variant="outline">
                <Link href={ROUTES.problems}>Xem bài tập</Link>
            </Button>
        </div>
    );
}
