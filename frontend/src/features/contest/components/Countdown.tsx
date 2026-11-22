"use client";

import { countdownParts } from "../countdown";
import { useNow } from "@/lib/useNow";

interface CountdownProps {
    label: string;
    target: string;
}

export function Countdown({ label, target }: CountdownProps) {
    const now = useNow();

    return (
        <div className="flex flex-col gap-2 lg:items-end">
            <p className="font-semibold text-contest-hero-muted">{label}</p>
            <div role="timer" aria-label={label} className="flex gap-2.5">
                {countdownParts(target, now).map((part) => (
                    <div
                        key={part.unit}
                        className="min-w-[88px] rounded-lg border border-contest-hero-line bg-contest-hero-panel px-3 py-3 text-center"
                    >
                        <span className="block text-[40px] leading-none font-extrabold tabular-nums">
                            {part.value}
                        </span>
                        <span className="mt-1.5 block text-sm text-contest-hero-muted">
                            {part.unit}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
}
