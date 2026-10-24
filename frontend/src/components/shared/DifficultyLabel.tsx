import { DIFFICULTY_LABELS, DIFFICULTY_TEXT_CLASSES, type Difficulty } from "@/lib/difficulty";
import { cn } from "@/lib/utils";

interface DifficultyLabelProps {
    difficulty: Difficulty;
    className?: string;
}

export function DifficultyLabel({ difficulty, className }: DifficultyLabelProps) {
    return (
        <span className={cn("font-semibold", DIFFICULTY_TEXT_CLASSES[difficulty], className)}>
            {DIFFICULTY_LABELS[difficulty]}
        </span>
    );
}
