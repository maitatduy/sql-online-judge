import Link from "next/link";

import { isOAuthEnabled } from "@/lib/feature-flags";

import { AUTH_CONTENT } from "../content";
import type { AuthMode } from "../types";

import { AuthTabs } from "./AuthTabs";
import { OAuthButtons } from "./OAuthButtons";

interface AuthCardProps {
    mode: AuthMode;
    nextPath: string;
    children?: React.ReactNode;
}

export function AuthCard({ mode, nextPath, children }: AuthCardProps) {
    const content = AUTH_CONTENT[mode];

    return (
        <div className="flex flex-col gap-5 rounded-xl border bg-card p-6 shadow-2xl sm:p-8">
            <AuthTabs mode={mode} />
            <div>
                <h2 className="text-[26px] font-extrabold tracking-[-0.02em]">{content.title}</h2>
                <p className="mt-1.5 text-sm text-muted-foreground">{content.subtitle}</p>
            </div>
            {isOAuthEnabled() && (
                <>
                    <OAuthButtons nextPath={nextPath} />
                    <p className="flex items-center gap-3 text-xs text-muted-foreground">
                        <span aria-hidden="true" className="h-px flex-1 bg-border" />
                        hoặc dùng email
                        <span aria-hidden="true" className="h-px flex-1 bg-border" />
                    </p>
                </>
            )}
            {children}
            <p className="text-center text-[13px] text-muted-foreground">
                {content.switchQuestion}{" "}
                <Link
                    href={content.switchHref}
                    className="inline-flex min-h-11 items-center font-semibold text-brand underline-offset-4 hover:underline md:min-h-0"
                >
                    {content.switchLabel}
                </Link>
            </p>
        </div>
    );
}
