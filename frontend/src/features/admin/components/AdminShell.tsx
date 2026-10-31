"use client";

import { RoleGate } from "@/components/shared/RoleGate";

import { AdminFrame } from "./AdminFrame";

const ADMIN_ROLES = ["ADMIN"] as const;

interface AdminShellProps {
    children: React.ReactNode;
}

export function AdminShell({ children }: AdminShellProps) {
    return (
        <RoleGate
            allowedRoles={ADMIN_ROLES}
            requiredRole="ADMIN"
            forbiddenDescription="Trang này dành cho ban quản trị."
            errorMessage="Trang quản trị chưa tải được. Bạn thử lại sau ít phút nhé."
        >
            {(user) => <AdminFrame user={user}>{children}</AdminFrame>}
        </RoleGate>
    );
}
