"use client";

import { useState } from "react";

import { Plus } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";

import { useLessonQuiz } from "../hooks/useLessonQuiz";
import { QuizQuestionCard } from "./QuizQuestionCard";
import { QuizQuestionForm } from "./QuizQuestionForm";

interface LessonQuizEditorProps {
    lessonId: string;
    isLocked: boolean;
}

export function LessonQuizEditor({ lessonId, isLocked }: LessonQuizEditorProps) {
    const { data: questions, error, isPending, refetch } = useLessonQuiz(lessonId);
    const [isAdding, setIsAdding] = useState(false);

    return (
        <section aria-labelledby="lesson-quiz-title" className="flex flex-col gap-3">
            <h2 id="lesson-quiz-title" className="text-lg font-bold">
                Quiz
            </h2>
            {isPending ? (
                <div role="status" aria-label="Đang tải quiz">
                    <Skeleton className="h-32" />
                </div>
            ) : null}
            {error && !questions ? (
                <ErrorState
                    title="Không tải được quiz"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {questions && questions.length === 0 ? (
                <EmptyState title="Bài giảng chưa có câu hỏi nào." />
            ) : null}
            {questions?.map((question, index) => (
                <QuizQuestionCard
                    key={question.id}
                    lessonId={lessonId}
                    question={question}
                    index={index + 1}
                    isLocked={isLocked}
                />
            ))}
            {isLocked || !questions ? null : isAdding ? (
                <QuizQuestionForm lessonId={lessonId} onDone={() => setIsAdding(false)} />
            ) : (
                <Button
                    type="button"
                    variant="outline"
                    className="w-fit"
                    onClick={() => setIsAdding(true)}
                >
                    <Plus aria-hidden="true" />
                    Thêm câu hỏi
                </Button>
            )}
        </section>
    );
}
