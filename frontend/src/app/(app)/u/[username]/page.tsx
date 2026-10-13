import type { Metadata } from "next";

import { ProfileView } from "@/features/profile/components/ProfileView";

interface ProfilePageProps {
    params: Promise<{ username: string }>;
}

async function readUsername(params: ProfilePageProps["params"]): Promise<string> {
    const { username } = await params;
    return decodeURIComponent(username);
}

export async function generateMetadata({ params }: ProfilePageProps): Promise<Metadata> {
    return { title: `Hồ sơ - @${await readUsername(params)}` };
}

export default async function ProfilePage({ params }: ProfilePageProps) {
    return <ProfileView username={await readUsername(params)} />;
}
