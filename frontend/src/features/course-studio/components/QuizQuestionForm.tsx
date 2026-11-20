"use client";

import { useState } from "react";

import { Plus, X } from "lucide-react";

import { IconAction } from "@/components/shared/IconAction";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import { useSaveQuizQuestion } from "../hooks/useLessonQuiz";
import { MAX_QUIZ_OPTIONS, MIN_QUIZ_OPTIONS, type QuizProblem, quizProblemOf } from "../quiz-rules";
import { serverFieldErrorOf } from "../server-errors";
import type { QuizOptionDraft, StudioQuizQuestion } from "../types";

const EMPTY_OPTIONS: QuizOptionDraft[] = [
    { label: "", correct: true },
    { label: "", correct: false },
];

interface QuizQuestionFormProps {
    lessonId: string;
    question?: StudioQuizQuestion;
    onDone: () => void;
}

export function QuizQuestionForm({ lessonId, question, onDone }: QuizQuestionFormProps) {
    const showToast = useToast();
    const save = useSaveQuizQuestion(lessonId, question?.id);
    const [prompt, setPrompt] = useState(question?.prompt ?? "");
    const [multiple, setMultiple] = useState(question?.multiple ?? false);
    const [options, setOptions] = useState<QuizOptionDraft[]>(
        question?.options.map(({ label, correct }) => ({ label, correct })) ?? EMPTY_OPTIONS,
    );
    const [problem, setProblem] = useState<QuizProblem | null>(null);
    const idPrefix = `quiz-${question?.id ?? "new"}`;

    function changeOption(index: number, change: Partial<QuizOptionDraft>) {
        setOptions((current) =>
            current.map((option, position) =>
                position === index ? { ...option, ...change } : option,
            ),
        );
    }

    function submitQuestion(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const request = { prompt, multiple, options };
        const clientProblem = quizProblemOf(request);
        setProblem(clientProblem);
        if (clientProblem) {
            return;
        }
        void save
            .mutateAsync(request)
            .then(onDone)
            .catch((error: Error) => {
                const message = serverFieldErrorOf(error, "options");
                if (message) {
                    setProblem({ field: "options", message });
                    return;
                }
                showToast(`Chưa lưu được câu hỏi. ${error.message}`, "error");
            });
    }

    return (
        <form
            noValidate
            onSubmit={submitQuestion}
            className="flex flex-col gap-3 rounded-md bg-surface-2 p-4"
        >
            <div className="flex flex-col gap-1.5">
                <Label htmlFor={`${idPrefix}-prompt`} className="text-sm">
                    Câu hỏi
                </Label>
                <textarea
                    id={`${idPrefix}-prompt`}
                    value={prompt}
                    aria-invalid={problem?.field === "prompt" ? true : undefined}
                    onChange={(event) => setPrompt(event.target.value)}
                    className="min-h-20 rounded-md border border-input bg-card px-3 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                />
                {problem?.field === "prompt" ? (
                    <p className="text-sm text-destructive">{problem.message}</p>
                ) : null}
            </div>
            <label className="flex items-center gap-2 text-sm">
                <input
                    type="checkbox"
                    checked={multiple}
                    onChange={(event) => setMultiple(event.target.checked)}
                    className="size-4 accent-brand"
                />
                Cho phép chọn nhiều đáp án
            </label>
            <fieldset className="flex flex-col gap-2">
                <legend className="mb-1.5 text-sm font-medium">Lựa chọn</legend>
                {options.map((option, index) => (
                    <div key={index} className="flex items-center gap-2">
                        <input
                            type="checkbox"
                            aria-label={`Lựa chọn ${index + 1} đúng`}
                            checked={option.correct}
                            onChange={(event) =>
                                changeOption(index, { correct: event.target.checked })
                            }
                            className="size-4 shrink-0 accent-brand"
                        />
                        <Input
                            aria-label={`Lựa chọn ${index + 1}`}
                            value={option.label}
                            onChange={(event) => changeOption(index, { label: event.target.value })}
                            className="h-10 bg-card text-base md:text-base"
                        />
                        {options.length > MIN_QUIZ_OPTIONS ? (
                            <IconAction
                                label={`Bỏ lựa chọn ${index + 1}`}
                                tooltip="Bỏ lựa chọn"
                                icon={X}
                                onClick={() =>
                                    setOptions((current) =>
                                        current.filter((_, position) => position !== index),
                                    )
                                }
                            />
                        ) : null}
                    </div>
                ))}
                {problem?.field === "options" ? (
                    <p className="text-sm text-destructive">{problem.message}</p>
                ) : null}
                {options.length < MAX_QUIZ_OPTIONS ? (
                    <Button
                        type="button"
                        variant="ghost"
                        className="w-fit"
                        onClick={() =>
                            setOptions((current) => [...current, { label: "", correct: false }])
                        }
                    >
                        <Plus aria-hidden="true" />
                        Thêm lựa chọn
                    </Button>
                ) : null}
            </fieldset>
            <div className="flex justify-end gap-2">
                <Button type="button" variant="ghost" onClick={onDone}>
                    Huỷ
                </Button>
                <Button type="submit" disabled={save.isPending}>
                    Lưu câu hỏi
                </Button>
            </div>
        </form>
    );
}
