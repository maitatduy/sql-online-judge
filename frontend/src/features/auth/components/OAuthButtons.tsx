"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { apiUrl } from "@/lib/api-client";
import { navigateBrowserTo } from "@/lib/browser-navigation";

import { saveOAuthNextPath } from "../oauth-redirect";
import type { OAuthProvider } from "../types";

import { BrandIcon } from "./BrandIcon";

interface OAuthButtonsProps {
    nextPath: string;
}

const PROVIDERS: { provider: OAuthProvider; label: string }[] = [
    { provider: "github", label: "GitHub" },
    { provider: "google", label: "Google" },
];

export function OAuthButtons({ nextPath }: OAuthButtonsProps) {
    const [connectingProvider, setConnectingProvider] = useState<OAuthProvider | null>(null);

    const handleProviderClick = (provider: OAuthProvider) => {
        setConnectingProvider(provider);
        saveOAuthNextPath(nextPath);
        navigateBrowserTo(apiUrl(`/api/auth/oauth2/authorization/${provider}`));
    };

    return (
        <div className="flex gap-2.5">
            {PROVIDERS.map(({ provider, label }) => {
                const isConnecting = connectingProvider === provider;
                return (
                    <Button
                        key={provider}
                        type="button"
                        variant="outline"
                        disabled={connectingProvider !== null}
                        aria-label={
                            isConnecting ? `Đang kết nối với ${label}…` : `Tiếp tục với ${label}`
                        }
                        onClick={() => handleProviderClick(provider)}
                        className="h-11.5 flex-1"
                    >
                        <BrandIcon brand={provider} />
                        {isConnecting ? "Đang kết nối…" : label}
                    </Button>
                );
            })}
        </div>
    );
}
