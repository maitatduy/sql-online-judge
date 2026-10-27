import { AppProviders } from "@/app/providers";

export default function WorkspaceLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return <AppProviders>{children}</AppProviders>;
}
