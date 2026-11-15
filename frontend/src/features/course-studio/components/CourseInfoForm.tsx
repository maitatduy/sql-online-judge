"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { FormProvider, useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import { useUpdateCourse } from "../hooks/useStudioCourse";
import { courseInfoSchema, type CourseInfoValues } from "../schemas";
import { serverFieldErrorOf } from "../server-errors";
import type { StudioCourse } from "../types";
import { CourseInfoFields } from "./CourseInfoFields";

const FIELDS = ["title", "summary", "difficulty"] as const;

interface CourseInfoFormProps {
    course: StudioCourse;
}

export function CourseInfoForm({ course }: CourseInfoFormProps) {
    const showToast = useToast();
    const updateCourse = useUpdateCourse(course.id);
    const isLocked = course.revisionStatus !== "DRAFT";
    const form = useForm<CourseInfoValues>({
        resolver: zodResolver(courseInfoSchema),
        defaultValues: {
            title: course.title,
            summary: course.summary,
            difficulty: course.difficulty,
        },
    });
    const {
        handleSubmit,
        reset,
        setError,
        formState: { isDirty },
    } = form;

    const submitUpdate = (values: CourseInfoValues) =>
        updateCourse
            .mutateAsync(values)
            .then((saved) => {
                reset(values);
                showToast("Đã lưu thông tin khoá.");
                return saved;
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
        <section aria-labelledby="course-info-title" className="rounded-lg border bg-card p-6">
            <h2 id="course-info-title" className="text-lg font-bold">
                Thông tin khoá
            </h2>
            {isLocked ? (
                <p className="mt-2 text-muted-foreground">Khoá đang chờ duyệt nên chưa sửa được.</p>
            ) : null}
            <FormProvider {...form}>
                <form noValidate onSubmit={handleSubmit(submitUpdate)}>
                    <fieldset disabled={isLocked} className="mt-4 flex flex-col gap-4">
                        <CourseInfoFields idPrefix="course" />
                    </fieldset>
                    {isLocked ? null : (
                        <div className="mt-5 flex justify-end border-t pt-4">
                            <Button
                                type="submit"
                                size="lg"
                                disabled={updateCourse.isPending || !isDirty}
                            >
                                Lưu thông tin
                            </Button>
                        </div>
                    )}
                </form>
            </FormProvider>
        </section>
    );
}
