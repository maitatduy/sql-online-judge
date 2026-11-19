"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toSlug } from "@/lib/slug";

import { useAddLesson } from "../hooks/useCourseOutline";
import { newLessonSchema, type NewLessonValues } from "../schemas";
import { serverFieldErrorOf } from "../server-errors";
import { CourseFormField } from "./CourseFormField";

const FIELDS = ["title", "slug", "estimatedMinutes"] as const;
const DEFAULT_MINUTES = 10;
const INPUT_CLASS = "h-10 bg-card text-base md:text-base";

interface NewLessonFormProps {
    chapterId: string;
    onDone: () => void;
}

export function NewLessonForm({ chapterId, onDone }: NewLessonFormProps) {
    const showToast = useToast();
    const addLesson = useAddLesson(chapterId);
    const {
        register,
        handleSubmit,
        setValue,
        getFieldState,
        setError,
        formState: { errors },
    } = useForm<NewLessonValues>({
        resolver: zodResolver(newLessonSchema),
        defaultValues: { title: "", slug: "", estimatedMinutes: DEFAULT_MINUTES },
    });

    const submitLesson = (values: NewLessonValues) =>
        addLesson
            .mutateAsync({ ...values, body: "" })
            .then(onDone)
            .catch((error: Error) => {
                const fieldErrors = FIELDS.flatMap((field) => {
                    const message = serverFieldErrorOf(error, field);
                    return message ? [{ field, message }] : [];
                });
                fieldErrors.forEach(({ field, message }) => setError(field, { message }));
                if (fieldErrors.length === 0) {
                    showToast(`Chưa thêm được bài giảng. ${error.message}`, "error");
                }
            });

    const idPrefix = `new-lesson-${chapterId}`;
    return (
        <form
            noValidate
            onSubmit={handleSubmit(submitLesson)}
            className="flex flex-col gap-3 rounded-md bg-surface-2 p-4"
        >
            <CourseFormField
                id={`${idPrefix}-title`}
                label="Tên bài giảng"
                error={errors.title?.message}
            >
                {(props) => (
                    <Input
                        autoComplete="off"
                        autoFocus
                        className={INPUT_CLASS}
                        {...props}
                        {...register("title", {
                            onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
                                if (!getFieldState("slug").isDirty) {
                                    setValue("slug", toSlug(event.target.value));
                                }
                            },
                        })}
                    />
                )}
            </CourseFormField>
            <div className="grid gap-3 sm:grid-cols-[1fr_10rem]">
                <CourseFormField id={`${idPrefix}-slug`} label="Slug" error={errors.slug?.message}>
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
                    id={`${idPrefix}-minutes`}
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
            <div className="flex justify-end gap-2">
                <Button type="button" variant="ghost" onClick={onDone}>
                    Huỷ
                </Button>
                <Button type="submit" disabled={addLesson.isPending}>
                    Lưu bài giảng
                </Button>
            </div>
        </form>
    );
}
