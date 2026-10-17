import { ChevronLeft } from "lucide-react";
import Link from "next/link";

import { ROUTES } from "@/lib/routes";

export function BackToLoginLink() {
    return (
        <Link
            href={ROUTES.login}
            className="inline-flex min-h-11 items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
        >
            <ChevronLeft aria-hidden="true" className="size-4" />
            Quay lại đăng nhập
        </Link>
    );
}
