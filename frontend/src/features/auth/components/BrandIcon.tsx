import type { OAuthProvider } from "../types";

interface BrandIconProps {
    brand: OAuthProvider;
}

interface BrandPath {
    d: string;
    fill?: string;
}

const BRAND_PATHS: Record<OAuthProvider, BrandPath[]> = {
    github: [
        {
            d: "M12 2a10 10 0 0 0-3.2 19.5c.5.1.7-.2.7-.5v-1.7c-2.8.6-3.4-1.3-3.4-1.3-.4-1.2-1.1-1.5-1.1-1.5-.9-.6.1-.6.1-.6 1 .1 1.5 1 1.5 1 .9 1.6 2.4 1.1 3 .9.1-.7.4-1.1.6-1.4-2.2-.2-4.6-1.1-4.6-5 0-1.1.4-2 1-2.7-.1-.3-.4-1.3.1-2.7 0 0 .8-.3 2.8 1a9.5 9.5 0 0 1 5 0c1.9-1.3 2.7-1 2.7-1 .6 1.4.2 2.4.1 2.7.6.7 1 1.6 1 2.7 0 3.9-2.3 4.7-4.6 5 .4.3.7.9.7 1.9V21c0 .3.2.6.7.5A10 10 0 0 0 12 2z",
        },
    ],
    google: [
        {
            d: "M21.6 12.2c0-.7-.1-1.4-.2-2H12v3.8h5.4a4.6 4.6 0 0 1-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.3z",
            fill: "#4285F4",
        },
        {
            d: "M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1-2.6 0-4.8-1.8-5.6-4.1H3.1v2.6A10 10 0 0 0 12 22z",
            fill: "#34A853",
        },
        {
            d: "M6.4 14a6 6 0 0 1 0-3.9V7.5H3.1a10 10 0 0 0 0 9z",
            fill: "#FBBC05",
        },
        {
            d: "M12 6c1.5 0 2.8.5 3.8 1.5l2.9-2.9A10 10 0 0 0 3.1 7.5l3.3 2.6C7.2 7.8 9.4 6 12 6z",
            fill: "#EA4335",
        },
    ],
};

export function BrandIcon({ brand }: BrandIconProps) {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" className="size-4.5 fill-current">
            {BRAND_PATHS[brand].map((path) => (
                <path key={path.d} d={path.d} fill={path.fill} />
            ))}
        </svg>
    );
}
