"use client";

import { RoleGate } from "@/components/shared/RoleGate";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { TopBar } from "@/components/shared/TopBar";

const STUDIO_ROLES = ["SETTER", "ADMIN"] as const;

interface StudioShellProps {
    children: React.ReactNode;
}

export function StudioShell({ children }: StudioShellProps) {
    return (
        <RoleGate
            allowedRoles={STUDIO_ROLES}
            requiredRole="SETTER"
            forbiddenDescription="Trang này dành cho người ra đề."
            errorMessage="Khu vực người ra đề chưa tải được. Bạn thử lại sau ít phút nhé."
        >
            {() => (
                <>
                    <TopBar />
                    <main id={MAIN_CONTENT_ID}>{children}</main>
                </>
            )}
        </RoleGate>
    );
}
