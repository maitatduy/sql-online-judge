import type { Metadata } from "next";
import Link from "next/link";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { SqlKeyword } from "@/components/shared/SqlKeyword";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

export const metadata: Metadata = {
    title: "Không tìm thấy trang",
};

export default function AppNotFound() {
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
            <p className="mt-3 font-mono text-[13px]">
                <SqlKeyword>SELECT</SqlKeyword> * <SqlKeyword>FROM</SqlKeyword> pages{" "}
                <SqlKeyword>WHERE</SqlKeyword> url ={" "}
                <span className="text-syntax-string">&apos;/this-page&apos;</span>;
            </p>
            <p className="mt-1 text-[13px] text-muted-foreground">
                0 dòng. Trang bạn tìm không tồn tại hoặc đã bị xoá.
            </p>
            <div className="mt-6 flex flex-wrap justify-center gap-2">
                <Button asChild variant="outline">
                    <Link href={ROUTES.home}>Về trang chủ</Link>
                </Button>
                <Button asChild>
                    <Link href={ROUTES.problems}>Xem bài tập</Link>
                </Button>
            </div>
        </main>
    );
}
