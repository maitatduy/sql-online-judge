import type { Metadata } from "next";
import { Be_Vietnam_Pro, JetBrains_Mono } from "next/font/google";

import { SITE_NAME } from "@/lib/site";
import "@/styles/globals.css";

const beVietnamPro = Be_Vietnam_Pro({
    variable: "--font-be-vietnam-pro",
    subsets: ["latin", "vietnamese"],
    weight: ["400", "500", "600", "700", "800"],
    display: "swap",
});

const jetBrainsMono = JetBrains_Mono({
    variable: "--font-jetbrains-mono",
    subsets: ["latin", "vietnamese"],
    display: "swap",
});

export const metadata: Metadata = {
    title: {
        default: SITE_NAME,
        template: `%s | ${SITE_NAME}`,
    },
    description: "Nền tảng học SQL có máy chấm tự động.",
};

export default function RootLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <html lang="vi" className={`${beVietnamPro.variable} ${jetBrainsMono.variable}`}>
            <body className="antialiased">{children}</body>
        </html>
    );
}
