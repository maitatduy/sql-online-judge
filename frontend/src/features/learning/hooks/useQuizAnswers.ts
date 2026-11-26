import { useState } from "react";

const ANSWERS_KEY_PREFIX = "sqljudge:lesson-quiz:";

export type QuizChoices = Record<string, string[]>;
export type QuizVerdicts = Record<string, boolean>;

interface QuizAnswers {
    choices: QuizChoices;
    verdicts: QuizVerdicts;
}

const NO_ANSWERS: QuizAnswers = { choices: {}, verdicts: {} };

function readAnswers(lessonKey: string): QuizAnswers {
    try {
        const stored = localStorage.getItem(ANSWERS_KEY_PREFIX + lessonKey);
        return stored ? (JSON.parse(stored) as QuizAnswers) : NO_ANSWERS;
    } catch {
        return NO_ANSWERS;
    }
}

function writeAnswers(lessonKey: string, answers: QuizAnswers): void {
    try {
        localStorage.setItem(ANSWERS_KEY_PREFIX + lessonKey, JSON.stringify(answers));
    } catch {
        return;
    }
}

export function useQuizAnswers(lessonKey: string) {
    const [answers, setAnswers] = useState<QuizAnswers>(() => readAnswers(lessonKey));

    function update(next: (current: QuizAnswers) => QuizAnswers) {
        setAnswers((current) => {
            const updated = next(current);
            writeAnswers(lessonKey, updated);
            return updated;
        });
    }

    return {
        choices: answers.choices,
        verdicts: answers.verdicts,
        choose: (questionKey: string, selected: string[]) =>
            update((current) => ({
                choices: { ...current.choices, [questionKey]: selected },
                verdicts: Object.fromEntries(
                    Object.entries(current.verdicts).filter(([key]) => key !== questionKey),
                ),
            })),
        recordVerdicts: (verdicts: QuizVerdicts) => update((current) => ({ ...current, verdicts })),
    };
}
