import { cn } from "@/lib/utils";

interface SpinnerProps {
    className?: string;
}

const RADIUS = 20;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;
const ARC_LENGTH = CIRCUMFERENCE / 4;

export function Spinner({ className }: SpinnerProps) {
    return (
        <svg
            viewBox="0 0 48 48"
            fill="none"
            aria-hidden="true"
            data-testid="spinner"
            className={cn("motion-safe:animate-spin", className)}
        >
            <circle
                cx="24"
                cy="24"
                r={RADIUS}
                stroke="currentColor"
                strokeWidth="4"
                className="opacity-15"
            />
            <circle
                cx="24"
                cy="24"
                r={RADIUS}
                stroke="currentColor"
                strokeWidth="4"
                strokeLinecap="round"
                strokeDasharray={`${ARC_LENGTH} ${CIRCUMFERENCE}`}
            />
        </svg>
    );
}
