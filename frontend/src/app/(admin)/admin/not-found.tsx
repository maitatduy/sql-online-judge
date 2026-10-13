import Link from "next/link";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

export default function AdminNotFound() {
    return (
        <main id={MAIN_CONTENT_ID} className="mx-auto max-w-xl px-6 py-24 text-center">
            <p
                aria-hidden="true"
                className="font-mono text-[56px] leading-none font-extrabold text-muted-foreground/40"
            >
                404
            </p>
            <h1 className="mt-4 text-2xl font-extrabold tracking-[-0.02em]">
                Không tìm thấy trang
            </h1>
            <p className="mt-3 text-[15px] leading-[1.6] text-muted-foreground">
                Trang quản trị này không tồn tại hoặc chưa được xây dựng.
            </p>
            <Button asChild className="mt-6">
                <Link href={ROUTES.admin}>Về Tổng quan</Link>
            </Button>
        </main>
    );
}
