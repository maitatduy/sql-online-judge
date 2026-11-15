import { useSearchParams } from "next/navigation";

import { DIFFICULTIES, type Difficulty } from "@/lib/difficulty";
import { ROUTES } from "@/lib/routes";

function difficultyOf(value: string | null): Difficulty | undefined {
    return DIFFICULTIES.find((difficulty) => difficulty === value);
}

function changeDifficulty(difficulty: Difficulty | undefined): void {
    const href = difficulty ? `${ROUTES.learn}?difficulty=${difficulty}` : ROUTES.learn;
    window.history.replaceState(null, "", href);
}

export function useDifficultyFilter() {
    const difficulty = difficultyOf(useSearchParams().get("difficulty"));

    return { difficulty, changeDifficulty };
}
