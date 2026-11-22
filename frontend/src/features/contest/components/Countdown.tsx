"use client";

import { countdownParts } from "../countdown";
import { useNow } from "../hooks/useNow";

interface CountdownProps {
    label: string;
    target: string;
}

export function Countdown({ label, target }: CountdownProps) {
    const now = useNow();

    return (
        <div className="flex flex-col gap-2">
            <p className="text-contest-hero-muted">{label}</p>
            <div role="timer" aria-label={label} className="flex gap-2">
                {countdownParts(target, now).map((part) => (
                    <div
                        key={part.unit}
                        className="min-w-[76px] rounded-lg border border-contest-hero-line bg-contest-hero-panel px-3 py-2.5 text-center"
                    >
                        <span className="block text-[34px] leading-tight font-extrabold tabular-nums text-contest-hero-text">
                            {part.value}
                        </span>
                        <span className="text-sm text-contest-hero-muted">{part.unit}</span>
                    </div>
                ))}
            </div>
        </div>
    );
}
