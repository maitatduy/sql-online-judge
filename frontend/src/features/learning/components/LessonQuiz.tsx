"use client";

import { CircleCheck, CircleX, ListChecks } from "lucide-react";

import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";

import { useQuizAnswers } from "../hooks/useQuizAnswers";
import { useSubmitQuiz } from "../hooks/useSubmitQuiz";
import type { QuizQuestion } from "../types";

interface LessonQuizProps {
    courseSlug: string;
    lessonSlug: string;
    lessonKey: string;
    questions: QuizQuestion[];
}

function toggled(selected: string[], optionKey: string, multiple: boolean): string[] {
    if (!multiple) {
        return [optionKey];
    }
    return selected.includes(optionKey)
        ? selected.filter((key) => key !== optionKey)
        : [...selected, optionKey];
}

export function LessonQuiz({ courseSlug, lessonSlug, lessonKey, questions }: LessonQuizProps) {
    const { choices, verdicts, choose: recordChoice, recordVerdicts } = useQuizAnswers(lessonKey);
    const submitQuiz = useSubmitQuiz(courseSlug, lessonSlug);
    const hasChoice = Object.values(choices).some((selected) => selected.length > 0);
    const isAllCorrect = questions.every((question) => verdicts[question.questionKey] === true);

    function choose(question: QuizQuestion, optionKey: string) {
        recordChoice(
            question.questionKey,
            toggled(choices[question.questionKey] ?? [], optionKey, question.multiple),
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
                    recordVerdicts(
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
            <div className="flex flex-wrap items-center justify-between gap-2 border-b pb-3">
                <h2 className="inline-flex items-center gap-2 text-base font-bold">
                    <ListChecks aria-hidden="true" className="size-5 text-brand" />
                    Kiểm tra nhanh
                </h2>
                {isAllCorrect ? (
                    <span className="inline-flex items-center gap-1 text-[13px] font-semibold text-brand">
                        <CircleCheck aria-hidden="true" className="size-4" />
                        Đã trả lời đúng tất cả
                    </span>
                ) : (
                    <span className="text-[13px] text-muted-foreground">
                        {`${formatNumber(questions.length)} câu`}
                    </span>
                )}
            </div>
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
