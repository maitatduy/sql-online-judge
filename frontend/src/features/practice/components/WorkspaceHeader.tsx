import { ChevronLeft } from "lucide-react";
import Link from "next/link";

import { Logo } from "@/components/shared/Logo";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

interface WorkspaceHeaderProps {
    title: string | null;
    children?: React.ReactNode;
}

export function WorkspaceHeader({ title, children }: WorkspaceHeaderProps) {
    return (
        <header className="flex h-12 shrink-0 items-center gap-3 border-b bg-card px-3">
            <Button asChild variant="ghost" size="icon-sm">
                <Link href={ROUTES.problems} aria-label="Quay lại danh sách bài">
                    <ChevronLeft aria-hidden="true" />
                </Link>
            </Button>
            <Link href={ROUTES.home} aria-label="Trang chủ" className="max-sm:hidden">
                <Logo className="[&>span]:hidden [&>svg]:size-7" />
            </Link>
            {title ? <span className="min-w-0 truncate font-semibold">{title}</span> : null}
            {children ? <div className="ml-auto flex items-center gap-2">{children}</div> : null}
        </header>
    );
}
