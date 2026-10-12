interface OAuthFlagEnvironment {
    nodeEnv: string | undefined;
    oauthFlag: string | undefined;
}

export function isOAuthEnabled(
    environment: OAuthFlagEnvironment = {
        nodeEnv: process.env.NODE_ENV,
        oauthFlag: process.env.NEXT_PUBLIC_OAUTH_ENABLED,
    },
): boolean {
    return environment.nodeEnv !== "production" || environment.oauthFlag === "true";
}
