"use client";

import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

interface ErrorPageProps {
    error: Error & { digest?: string };
    reset: () => void;
}

export default function ErrorPage({ reset }: ErrorPageProps) {
    return (
        <main className="flex min-h-screen flex-col items-center justify-center gap-4 px-6 text-center">
            <h1 className="text-3xl font-extrabold tracking-[-0.02em]">Đã có lỗi xảy ra</h1>
            <p className="max-w-md text-[17px] leading-[1.6] text-muted-foreground">
                Trang chưa tải được. Bạn thử lại sau ít phút nhé.
            </p>
            <div className="mt-4 flex flex-wrap justify-center gap-3">
                <Button type="button" size="lg" onClick={reset}>
                    Thử lại
                </Button>
                <Button asChild size="lg" variant="outline">
                    <Link href={ROUTES.home}>Về trang chủ</Link>
                </Button>
            </div>
        </main>
    );
}
