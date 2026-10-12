import type { Metadata } from "next";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

export const metadata: Metadata = {
    title: "Không tìm thấy trang",
};

export default function NotFound() {
    return (
        <main className="flex min-h-screen flex-col items-center justify-center gap-4 px-6 text-center">
            <p className="font-mono text-2xl font-semibold text-muted-foreground">404</p>
            <h1 className="text-3xl font-extrabold">Không tìm thấy trang</h1>
            <p className="max-w-md text-muted-foreground">
                Trang bạn tìm không tồn tại hoặc đã được chuyển đi.
            </p>
            <Button asChild>
                <Link href={ROUTES.home}>Về trang chủ</Link>
            </Button>
        </main>
    );
}
