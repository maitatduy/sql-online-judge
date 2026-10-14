import type { Metadata } from "next";

import { AdminUserPage } from "@/features/admin/components/users/AdminUserPage";

interface AdminUserRouteProps {
    params: Promise<{ username: string }>;
}

async function readUsername(params: AdminUserRouteProps["params"]): Promise<string> {
    const { username } = await params;
    return decodeURIComponent(username);
}

export async function generateMetadata({ params }: AdminUserRouteProps): Promise<Metadata> {
    return { title: `Người dùng - ${await readUsername(params)}` };
}

export default async function AdminUserRoute({ params }: AdminUserRouteProps) {
    return <AdminUserPage username={await readUsername(params)} />;
}
