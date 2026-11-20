"use client";

import { useState } from "react";

import { Check, Pencil, Trash2 } from "lucide-react";

import { DeleteConfirmDialog } from "@/components/shared/DeleteConfirmDialog";
import { IconAction } from "@/components/shared/IconAction";
import { useToast } from "@/components/shared/ToastProvider";

import { useDeleteQuizQuestion } from "../hooks/useLessonQuiz";
import type { StudioQuizQuestion } from "../types";
import { QuizQuestionForm } from "./QuizQuestionForm";

interface QuizQuestionCardProps {
    lessonId: string;
    question: StudioQuizQuestion;
    index: number;
    isLocked: boolean;
}

export function QuizQuestionCard({ lessonId, question, index, isLocked }: QuizQuestionCardProps) {
    const showToast = useToast();
    const deleteQuestion = useDeleteQuizQuestion(lessonId);
    const [isEditing, setIsEditing] = useState(false);
    const [isConfirming, setIsConfirming] = useState(false);

    function confirmDelete() {
        void deleteQuestion
            .mutateAsync(question.id)
            .catch((error: Error) => showToast(error.message, "error"))
            .finally(() => setIsConfirming(false));
    }

    return (
        <div role="group" aria-label={`Câu ${index}`} className="rounded-lg border bg-card">
            <div className="flex min-h-12 items-center gap-2 border-b px-4 py-2">
                <h3 className="flex-1 font-bold">Câu {index}</h3>
                <span className="rounded-full bg-surface-2 px-2.5 py-0.5 text-sm font-semibold text-muted-foreground">
                    {question.multiple ? "Nhiều đáp án" : "Một đáp án"}
                </span>
                {isLocked ? null : (
                    <>
                        <IconAction
                            label={`Sửa câu ${index}`}
                            tooltip="Sửa"
                            icon={Pencil}
                            onClick={() => setIsEditing(true)}
                        />
                        <IconAction
                            label={`Xoá câu ${index}`}
                            tooltip="Xoá"
                            icon={Trash2}
                            onClick={() => setIsConfirming(true)}
                        />
                    </>
                )}
            </div>
            <div className="p-4">
                {isEditing ? (
                    <QuizQuestionForm
                        lessonId={lessonId}
                        question={question}
                        onDone={() => setIsEditing(false)}
                    />
                ) : (
                    <>
                        <p className="font-semibold">{question.prompt}</p>
                        <ul className="mt-2 flex flex-col gap-1.5">
                            {question.options.map((option, optionIndex) => (
                                <li
                                    key={optionIndex}
                                    className="flex items-center gap-2 rounded-md bg-surface-2 px-3 py-2"
                                >
                                    <span className="flex-1">{option.label}</span>
                                    {option.correct ? (
                                        <span className="inline-flex items-center gap-1 text-sm font-semibold text-brand">
                                            <Check aria-hidden="true" className="size-4" />
                                            Đúng
                                        </span>
                                    ) : null}
                                </li>
                            ))}
                        </ul>
                    </>
                )}
            </div>
            <DeleteConfirmDialog
                open={isConfirming}
                onOpenChange={setIsConfirming}
                title={`Xoá câu ${index}?`}
                description="Câu hỏi và các lựa chọn sẽ bị xoá."
                confirmLabel="Xoá câu hỏi"
                isPending={deleteQuestion.isPending}
                onConfirm={confirmDelete}
            />
        </div>
    );
}
