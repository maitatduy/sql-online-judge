import Link from "next/link";

import { Breadcrumb, type BreadcrumbItem } from "@/components/shared/Breadcrumb";
import { Logo } from "@/components/shared/Logo";
import { ROUTES } from "@/lib/routes";

interface WorkspaceHeaderProps {
    trail: BreadcrumbItem[];
    title: string | null;
    children?: React.ReactNode;
}

export function WorkspaceHeader({ trail, title, children }: WorkspaceHeaderProps) {
    return (
        <header className="flex h-12 shrink-0 items-center gap-3 border-b bg-card px-3">
            <Link href={ROUTES.home} aria-label="Trang chủ" className="shrink-0">
                <Logo className="[&>span]:hidden [&>svg]:size-7" />
            </Link>
            <Breadcrumb items={title ? [...trail, { label: title }] : trail} />
            {children ? <div className="ml-auto flex items-center gap-2">{children}</div> : null}
        </header>
    );
}
