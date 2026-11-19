"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { FormProvider, useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useUnsavedChangesWarning } from "@/lib/useUnsavedChangesWarning";

import { useUpdateLesson } from "../hooks/useStudioLesson";
import { lessonSchema, type LessonValues } from "../schemas";
import { serverFieldErrorOf } from "../server-errors";
import type { StudioLesson } from "../types";
import { CourseFormField } from "./CourseFormField";
import { LessonBodyField } from "./LessonBodyField";

const FIELDS = ["title", "slug", "estimatedMinutes", "body"] as const;
const INPUT_CLASS = "h-10 text-base md:text-base";

interface LessonFormProps {
    lesson: StudioLesson;
    isLocked: boolean;
}

export function LessonForm({ lesson, isLocked }: LessonFormProps) {
    const showToast = useToast();
    const updateLesson = useUpdateLesson(lesson.id);
    const form = useForm<LessonValues>({
        resolver: zodResolver(lessonSchema),
        defaultValues: {
            title: lesson.title,
            slug: lesson.slug,
            estimatedMinutes: lesson.estimatedMinutes,
            body: lesson.body,
        },
    });
    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: { errors, isDirty },
    } = form;
    useUnsavedChangesWarning(isDirty);

    const submitLesson = (values: LessonValues) =>
        updateLesson
            .mutateAsync(values)
            .then(() => {
                reset(values);
                showToast("Đã lưu bài giảng.");
            })
            .catch((error: Error) => {
                const fieldErrors = FIELDS.flatMap((field) => {
                    const message = serverFieldErrorOf(error, field);
                    return message ? [{ field, message }] : [];
                });
                fieldErrors.forEach(({ field, message }) => setError(field, { message }));
                if (fieldErrors.length === 0) {
                    showToast(`Chưa lưu được. ${error.message}`, "error");
                }
            });

    return (
        <FormProvider {...form}>
            <form noValidate onSubmit={handleSubmit(submitLesson)}>
                <fieldset
                    disabled={isLocked}
                    className="flex flex-col gap-4 rounded-lg border bg-card p-6"
                >
                    <CourseFormField
                        id="lesson-title"
                        label="Tên bài giảng"
                        error={errors.title?.message}
                    >
                        {(props) => (
                            <Input
                                autoComplete="off"
                                className={INPUT_CLASS}
                                {...props}
                                {...register("title")}
                            />
                        )}
                    </CourseFormField>
                    <div className="grid gap-4 sm:grid-cols-[1fr_10rem]">
                        <CourseFormField id="lesson-slug" label="Slug" error={errors.slug?.message}>
                            {(props) => (
                                <Input
                                    autoComplete="off"
                                    className={INPUT_CLASS}
                                    {...props}
                                    {...register("slug")}
                                />
                            )}
                        </CourseFormField>
                        <CourseFormField
                            id="lesson-minutes"
                            label="Thời lượng (phút)"
                            error={errors.estimatedMinutes?.message}
                        >
                            {(props) => (
                                <Input
                                    type="number"
                                    inputMode="numeric"
                                    className={INPUT_CLASS}
                                    {...props}
                                    {...register("estimatedMinutes", { valueAsNumber: true })}
                                />
                            )}
                        </CourseFormField>
                    </div>
                    <LessonBodyField />
                    {isLocked ? null : (
                        <div className="flex justify-end border-t pt-4">
                            <Button
                                type="submit"
                                size="lg"
                                disabled={updateLesson.isPending || !isDirty}
                            >
                                Lưu bài giảng
                            </Button>
                        </div>
                    )}
                </fieldset>
            </form>
        </FormProvider>
    );
}
