import Link from "next/link";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { SqlKeyword } from "@/components/shared/SqlKeyword";
import { Button } from "@/components/ui/button";
import type { Role } from "@/lib/current-user";
import { ROUTES } from "@/lib/routes";

interface ForbiddenPageProps {
    username: string;
    requiredRole: Role;
    description: string;
}

export function ForbiddenPage({ username, requiredRole, description }: ForbiddenPageProps) {
    return (
        <main id={MAIN_CONTENT_ID} className="mx-auto max-w-xl px-6 py-24 text-center">
            <p
                aria-hidden="true"
                className="font-mono text-[56px] leading-none font-extrabold text-muted-foreground/40"
            >
                403
            </p>
            <h1 className="mt-4 text-2xl font-extrabold tracking-[-0.02em]">
                Bạn không có quyền xem trang này
            </h1>
            <p className="mt-3 text-[15px] leading-[1.6] text-muted-foreground">{description}</p>
            <p className="mt-4 font-mono text-[13px] break-words">
                <SqlKeyword>SELECT</SqlKeyword> * <SqlKeyword>FROM</SqlKeyword> roles{" "}
                <SqlKeyword>WHERE</SqlKeyword> username ={" "}
                <span className="text-syntax-string">&apos;{username}&apos;</span>{" "}
                <SqlKeyword>AND</SqlKeyword> role ={" "}
                <span className="text-syntax-string">&apos;{requiredRole}&apos;</span>;
            </p>
            <p className="mt-1 text-[13px] text-muted-foreground">
                0 dòng. Cần vai trò {requiredRole}.
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
