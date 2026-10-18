import { useEffect, useState } from "react";

const TICK_MS = 1000;

export function useCountdown(durationSeconds: number) {
    const [deadline, setDeadline] = useState(() => Date.now() + durationSeconds * TICK_MS);
    const [now, setNow] = useState(() => Date.now());
    const secondsLeft = Math.max(0, Math.ceil((deadline - now) / TICK_MS));

    useEffect(() => {
        if (secondsLeft === 0) {
            return;
        }
        const timer = setInterval(() => setNow(Date.now()), TICK_MS);
        return () => clearInterval(timer);
    }, [secondsLeft]);

    const restart = () => {
        const startedAt = Date.now();
        setNow(startedAt);
        setDeadline(startedAt + durationSeconds * TICK_MS);
    };

    return { secondsLeft, restart };
}
