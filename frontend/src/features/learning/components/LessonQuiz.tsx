"use client";

import { CircleCheck, CircleX } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";

import { useSubmitQuiz } from "../hooks/useSubmitQuiz";
import type { QuizQuestion } from "../types";

interface LessonQuizProps {
    courseSlug: string;
    lessonSlug: string;
    questions: QuizQuestion[];
}

type Choices = Record<string, string[]>;

function toggled(selected: string[], optionKey: string, multiple: boolean): string[] {
    if (!multiple) {
        return [optionKey];
    }
    return selected.includes(optionKey)
        ? selected.filter((key) => key !== optionKey)
        : [...selected, optionKey];
}

export function LessonQuiz({ courseSlug, lessonSlug, questions }: LessonQuizProps) {
    const [choices, setChoices] = useState<Choices>({});
    const [verdicts, setVerdicts] = useState<Record<string, boolean>>({});
    const submitQuiz = useSubmitQuiz(courseSlug, lessonSlug);
    const hasChoice = Object.values(choices).some((selected) => selected.length > 0);

    function choose(question: QuizQuestion, optionKey: string) {
        setChoices((current) => ({
            ...current,
            [question.questionKey]: toggled(
                current[question.questionKey] ?? [],
                optionKey,
                question.multiple,
            ),
        }));
        setVerdicts((current) =>
            Object.fromEntries(
                Object.entries(current).filter(([key]) => key !== question.questionKey),
            ),
        );
    }

    function submit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        submitQuiz.mutate(
            questions.map((question) => ({
                questionKey: question.questionKey,
                optionKeys: choices[question.questionKey] ?? [],
            })),
            {
                onSuccess: (result) =>
                    setVerdicts(
                        Object.fromEntries(
                            result.results.map((item) => [item.questionKey, item.correct]),
                        ),
                    ),
            },
        );
    }

    return (
        <form
            aria-label="Câu hỏi"
            onSubmit={submit}
            className="flex flex-col gap-5 rounded-lg border bg-card p-5"
        >
            <h2 className="text-base font-bold">Kiểm tra nhanh</h2>
            {questions.map((question) => (
                <QuizQuestionField
                    key={question.questionKey}
                    question={question}
                    selected={choices[question.questionKey] ?? []}
                    verdict={verdicts[question.questionKey]}
                    onChoose={(optionKey) => choose(question, optionKey)}
                />
            ))}
            <div className="flex flex-wrap items-center gap-3">
                <Button type="submit" disabled={!hasChoice || submitQuiz.isPending}>
                    Nộp bài
                </Button>
                {submitQuiz.isError ? (
                    <p role="alert" className="text-[13px] text-destructive">
                        Chưa nộp được, thử lại
                    </p>
                ) : null}
            </div>
        </form>
    );
}

interface QuizQuestionFieldProps {
    question: QuizQuestion;
    selected: string[];
    verdict: boolean | undefined;
    onChoose: (optionKey: string) => void;
}

function QuizQuestionField({ question, selected, verdict, onChoose }: QuizQuestionFieldProps) {
    return (
        <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 flex flex-wrap items-center gap-2 font-semibold">
                {question.prompt}
                {question.multiple ? (
                    <span className="text-[12px] font-normal text-muted-foreground">
                        Chọn nhiều
                    </span>
                ) : null}
                {verdict === true ? (
                    <span className="inline-flex items-center gap-1 text-[13px] text-brand">
                        <CircleCheck aria-hidden="true" className="size-4" />
                        Đúng
                    </span>
                ) : null}
                {verdict === false ? (
                    <span className="inline-flex items-center gap-1 text-[13px] text-destructive">
                        <CircleX aria-hidden="true" className="size-4" />
                        Chưa đúng
                    </span>
                ) : null}
            </legend>
            {question.options.map((option) => (
                <label
                    key={option.optionKey}
                    className="flex min-h-11 cursor-pointer items-center gap-3 rounded-md border px-3 py-2 transition-colors has-checked:border-brand has-checked:bg-brand-soft hover:bg-surface-2"
                >
                    <input
                        type={question.multiple ? "checkbox" : "radio"}
                        name={question.questionKey}
                        checked={selected.includes(option.optionKey)}
                        onChange={() => onChoose(option.optionKey)}
                        className="accent-brand"
                    />
                    <span>{option.label}</span>
                </label>
            ))}
        </fieldset>
    );
}
