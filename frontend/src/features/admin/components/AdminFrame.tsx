"use client";

import { usePathname } from "next/navigation";

import { AdminSidebar } from "@/components/shared/AdminSidebar";
import { AdminSidebarDrawer } from "@/components/shared/AdminSidebarDrawer";
import type { CurrentUser } from "@/lib/current-user";

import { useAdminNavigationCounts } from "../hooks/useAdminNavigationCounts";
import { useSidebarCollapsed } from "../hooks/useSidebarCollapsed";

interface AdminFrameProps {
    user: CurrentUser;
    children: React.ReactNode;
}

export function AdminFrame({ user, children }: AdminFrameProps) {
    const pathname = usePathname();
    const { data: counts } = useAdminNavigationCounts();
    const { isCollapsed, toggleCollapsed } = useSidebarCollapsed();

    return (
        <div className="flex min-h-screen flex-col lg:flex-row">
            <AdminSidebarDrawer activePath={pathname} counts={counts} user={user} />
            <div className="hidden lg:block">
                <AdminSidebar
                    activePath={pathname}
                    counts={counts}
                    user={user}
                    isCollapsed={isCollapsed}
                    onToggleCollapse={toggleCollapsed}
                />
            </div>
            <div className="min-w-0 flex-1">{children}</div>
        </div>
    );
}
