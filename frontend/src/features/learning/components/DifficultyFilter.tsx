import { DIFFICULTIES, DIFFICULTY_LABELS, type Difficulty } from "@/lib/difficulty";

interface DifficultyFilterProps {
    value: Difficulty | undefined;
    onChange: (difficulty: Difficulty | undefined) => void;
}

const OPTIONS: { value: Difficulty | undefined; label: string }[] = [
    { value: undefined, label: "Tất cả" },
    ...DIFFICULTIES.map((difficulty) => ({
        value: difficulty,
        label: DIFFICULTY_LABELS[difficulty],
    })),
];

export function DifficultyFilter({ value, onChange }: DifficultyFilterProps) {
    return (
        <fieldset className="flex flex-wrap gap-1.5">
            <legend className="sr-only">Độ khó</legend>
            {OPTIONS.map((option) => (
                <label
                    key={option.label}
                    className="inline-flex min-h-9 cursor-pointer items-center rounded-full border px-3.5 text-[13px] font-medium text-muted-foreground transition-colors has-checked:border-brand has-checked:bg-brand-soft has-checked:text-brand has-focus-visible:ring-2 has-focus-visible:ring-ring hover:text-foreground"
                >
                    <input
                        type="radio"
                        name="course-difficulty"
                        className="sr-only"
                        checked={value === option.value}
                        onChange={() => onChange(option.value)}
                    />
                    {option.label}
                </label>
            ))}
        </fieldset>
    );
}
