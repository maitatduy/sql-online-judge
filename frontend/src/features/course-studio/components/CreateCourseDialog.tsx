"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { FormProvider, useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { ROUTES } from "@/lib/routes";

import { useCreateCourse } from "../hooks/useStudioCourse";
import { createCourseSchema, type CreateCourseValues } from "../schemas";
import { serverFieldErrorOf } from "../server-errors";
import { CourseFormField } from "./CourseFormField";
import { CourseInfoFields } from "./CourseInfoFields";

const FIELDS = ["slug", "title", "summary", "difficulty"] as const;

interface CreateCourseDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function CreateCourseDialog({ open, onOpenChange }: CreateCourseDialogProps) {
    const router = useRouter();
    const showToast = useToast();
    const createCourse = useCreateCourse();
    const form = useForm<CreateCourseValues>({
        resolver: zodResolver(createCourseSchema),
        defaultValues: {
            slug: "",
            title: "",
            summary: "",
            difficulty: "EASY",
        },
    });
    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: { errors },
    } = form;

    function changeOpen(nextOpen: boolean) {
        if (!nextOpen) {
            reset();
        }
        onOpenChange(nextOpen);
    }

    const submitCreate = (values: CreateCourseValues) =>
        createCourse
            .mutateAsync(values)
            .then((created) => {
                showToast(`Đã tạo khoá nháp ${created.title}.`);
                router.push(ROUTES.studioCourse(created.id));
            })
            .catch((error: Error) => {
                const fieldErrors = FIELDS.flatMap((field) => {
                    const message = serverFieldErrorOf(error, field);
                    return message ? [{ field, message }] : [];
                });
                fieldErrors.forEach(({ field, message }) => setError(field, { message }));
                if (fieldErrors.length === 0) {
                    showToast(`Chưa tạo được khoá. ${error.message}`, "error");
                }
            });

    return (
        <Dialog open={open} onOpenChange={changeOpen}>
            <DialogContent className="sm:max-w-lg">
                <DialogTitle>Tạo khoá mới</DialogTitle>
                <DialogDescription className="text-base">
                    Khoá được lưu ở dạng nháp, bạn thêm chương và bài giảng sau.
                </DialogDescription>
                <FormProvider {...form}>
                    <form
                        noValidate
                        onSubmit={handleSubmit(submitCreate)}
                        className="flex flex-col gap-4"
                    >
                        <CourseInfoFields
                            idPrefix="new-course"
                            slugField={
                                <CourseFormField
                                    id="new-course-slug"
                                    label="Slug"
                                    error={errors.slug?.message}
                                >
                                    {(props) => (
                                        <Input
                                            autoComplete="off"
                                            placeholder="vi-du-sql-co-ban"
                                            className="h-10 text-base md:text-base"
                                            {...props}
                                            {...register("slug")}
                                        />
                                    )}
                                </CourseFormField>
                            }
                        />
                        <div className="flex justify-end gap-2">
                            <Button
                                type="button"
                                variant="outline"
                                onClick={() => changeOpen(false)}
                            >
                                Huỷ
                            </Button>
                            <Button type="submit" disabled={createCourse.isPending}>
                                Tạo khoá nháp
                            </Button>
                        </div>
                    </form>
                </FormProvider>
            </DialogContent>
        </Dialog>
    );
}
