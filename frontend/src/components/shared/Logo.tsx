import { cn } from "@/lib/utils";

interface LogoProps {
    className?: string;
}

export function Logo({ className }: LogoProps) {
    return (
        <span className={cn("inline-flex items-center gap-2.5 text-foreground", className)}>
            <svg viewBox="0 0 32 32" aria-hidden="true" className="size-8 shrink-0">
                <rect width="32" height="32" rx="7" className="fill-brand" />
                <g
                    fill="none"
                    strokeWidth="2.2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="stroke-white"
                >
                    <ellipse cx="16" cy="9" rx="8" ry="3" />
                    <path d="M8 9v14c0 1.66 3.58 3 8 3s8-1.34 8-3V9" />
                    <path d="M11.6 18.2l3.1 3 5.7-6.2" />
                </g>
            </svg>
            <span className="text-[17px] font-extrabold">SQL Judge</span>
        </span>
    );
}
