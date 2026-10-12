import { useId } from "react";

import { cn } from "@/lib/utils";

interface LogoProps {
    className?: string;
}

export function Logo({ className }: LogoProps) {
    const maskId = `logo-mask-${useId().replace(/[^a-zA-Z0-9]/g, "")}`;

    return (
        <span className={cn("inline-flex items-center gap-2.5 text-foreground", className)}>
            <svg viewBox="0 -45 512 512" aria-hidden="true" className="size-9 shrink-0">
                <defs>
                    <mask id={maskId}>
                        <rect x="0" y="-45" width="512" height="512" fill="white" />
                        <g fill="none" stroke="black" strokeWidth="12">
                            <path d="M2 66a155 58 0 0 0 310 0" />
                            <path d="M2 165a155 58 0 0 0 310 0" />
                            <path d="M2 265a155 58 0 0 0 310 0" />
                        </g>
                        <rect x="158" y="100" width="362" height="332" rx="54" fill="black" />
                    </mask>
                </defs>
                <g mask={`url(#${maskId})`} className="fill-brand">
                    <path d="M2 60V360a155 58 0 0 0 310 0V60Z" />
                    <ellipse cx="157" cy="60" rx="155" ry="58" />
                </g>
                <rect
                    x="170"
                    y="112"
                    width="338"
                    height="308"
                    rx="44"
                    strokeWidth="8"
                    className="fill-slate-900 stroke-white/25"
                />
                <text x="218" y="246" fontSize="112" fontWeight="700" className="fill-white">
                    SQL
                </text>
                <path
                    d="M230 300l45 30-45 33"
                    fill="none"
                    strokeWidth="16"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="stroke-white"
                />
                <rect x="295" y="352" width="107" height="17" rx="8.5" className="fill-brand" />
            </svg>
            <span className="text-[17px] font-extrabold">
                <span className="text-brand">SQL</span> Online Judge
            </span>
        </span>
    );
}
