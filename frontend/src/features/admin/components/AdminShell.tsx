"use client";

import { usePathname } from "next/navigation";

import { RoleGate } from "@/components/shared/RoleGate";
import { ADMIN_ROUTES } from "@/lib/admin-navigation";

import { AdminFrame } from "./AdminFrame";

const ADMIN_ROLES = ["ADMIN"] as const;
const REPORT_ROLES = ["ADMIN", "MODERATOR"] as const;

interface AdminShellProps {
    children: React.ReactNode;
}

export function AdminShell({ children }: AdminShellProps) {
    const isReportsPage = usePathname().startsWith(ADMIN_ROUTES.reports);

    return (
        <RoleGate
            allowedRoles={isReportsPage ? REPORT_ROLES : ADMIN_ROLES}
            requiredRole={isReportsPage ? "MODERATOR" : "ADMIN"}
            forbiddenDescription="Trang này dành cho ban quản trị."
            errorMessage="Trang quản trị chưa tải được. Bạn thử lại sau ít phút nhé."
        >
            {(user) => <AdminFrame user={user}>{children}</AdminFrame>}
        </RoleGate>
    );
}
