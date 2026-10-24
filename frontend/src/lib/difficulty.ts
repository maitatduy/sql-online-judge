export type Difficulty = "EASY" | "MEDIUM" | "HARD";

export const DIFFICULTIES: readonly Difficulty[] = ["EASY", "MEDIUM", "HARD"];

export const DIFFICULTY_LABELS: Record<Difficulty, string> = {
    EASY: "Dễ",
    MEDIUM: "Trung bình",
    HARD: "Khó",
};

export const DIFFICULTY_TEXT_CLASSES: Record<Difficulty, string> = {
    EASY: "text-difficulty-easy",
    MEDIUM: "text-difficulty-medium",
    HARD: "text-difficulty-hard",
};

export const DIFFICULTY_STROKE_CLASSES: Record<Difficulty, string> = {
    EASY: "stroke-difficulty-easy",
    MEDIUM: "stroke-difficulty-medium",
    HARD: "stroke-difficulty-hard",
};
