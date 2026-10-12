import type { Metadata } from "next";
import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { Button } from "@/components/ui/button";
import { EditorPreview } from "@/features/marketing/components/EditorPreview";
import { SqlKeyword } from "@/features/marketing/components/SqlKeyword";
import { ROUTES } from "@/lib/routes";

export const metadata: Metadata = {
    title: "Không tìm thấy trang",
};

const RESULT_COLUMNS = ["title", "url"];

export default function NotFound() {
    return (
        <div className="flex min-h-screen flex-col">
            <header className="mx-auto w-full max-w-7xl px-6 py-4.5">
                <Link href={ROUTES.home} className="inline-flex min-h-11 items-center">
                    <Logo />
                </Link>
            </header>
            <main className="mx-auto grid w-full max-w-7xl flex-1 content-center items-center gap-10 px-6 py-12 lg:grid-cols-2 lg:gap-14">
                <div className="min-w-0">
                    <p className="font-mono text-7xl leading-none font-extrabold text-brand md:text-[120px]">
                        404
                    </p>
                    <h1 className="mt-6 text-3xl font-extrabold tracking-[-0.02em] md:text-[40px] md:leading-tight">
                        Không tìm thấy trang
                    </h1>
                    <p className="mt-4 max-w-md text-[17px] leading-[1.6] text-muted-foreground">
                        Truy vấn chạy đúng cú pháp nhưng không trả về dòng nào. Trang bạn tìm không
                        tồn tại hoặc đã được chuyển đi.
                    </p>
                    <Button asChild size="lg" className="mt-8">
                        <Link href={ROUTES.home}>Về trang chủ</Link>
                    </Button>
                </div>
                <EditorPreview
                    label="Truy vấn tìm trang không trả về dòng nào"
                    fileName="find_page.sql"
                    verdict="WA"
                    verdictDetail="404"
                    lineCount={4}
                    columns={RESULT_COLUMNS}
                    rows={[]}
                    size="lg"
                >
                    <SqlKeyword>SELECT</SqlKeyword> title, url{"\n"}
                    <SqlKeyword>FROM</SqlKeyword> pages{"\n"}
                    <SqlKeyword>WHERE</SqlKeyword> url ={" "}
                    <span className="text-syntax-string">&apos;/this-page&apos;</span>
                    {"\n"}
                    <SqlKeyword>LIMIT</SqlKeyword> <span className="text-syntax-number">1</span>;
                </EditorPreview>
            </main>
        </div>
    );
}
