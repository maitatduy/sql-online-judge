"use client";

import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";

import { getFormErrorMessage } from "../form-errors";
import { useOAuthLogin } from "../hooks/useOAuthLogin";
import type { OAuthProvider } from "../types";

import { BrandIcon } from "./BrandIcon";
import { FormAlert } from "./FormAlert";

interface OAuthButtonsProps {
    nextPath: string;
}

const PROVIDERS: { provider: OAuthProvider; label: string }[] = [
    { provider: "github", label: "GitHub" },
    { provider: "google", label: "Google" },
];

export function OAuthButtons({ nextPath }: OAuthButtonsProps) {
    const router = useRouter();
    const oauthLogin = useOAuthLogin();
    const isBusy = oauthLogin.isPending || oauthLogin.isSuccess;
    const connectingProvider = isBusy ? oauthLogin.variables : undefined;

    const handleProviderClick = (provider: OAuthProvider) => {
        oauthLogin.mutate(provider, { onSuccess: () => router.push(nextPath) });
    };

    return (
        <div className="flex flex-col gap-3">
            <div className="flex gap-2.5">
                {PROVIDERS.map(({ provider, label }) => {
                    const isConnecting = connectingProvider === provider;
                    return (
                        <Button
                            key={provider}
                            type="button"
                            variant="outline"
                            disabled={isBusy}
                            aria-label={
                                isConnecting
                                    ? `Đang kết nối với ${label}…`
                                    : `Tiếp tục với ${label}`
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
            {oauthLogin.isError && <FormAlert message={getFormErrorMessage(oauthLogin.error)} />}
        </div>
    );
}
