"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import { useAddChapter } from "../hooks/useCourseOutline";
import { chapterSchema, type ChapterValues } from "../schemas";

interface AddChapterFormProps {
    courseId: string;
}

export function AddChapterForm({ courseId }: AddChapterFormProps) {
    const showToast = useToast();
    const addChapter = useAddChapter(courseId);
    const {
        register,
        handleSubmit,
        reset,
        formState: { errors },
    } = useForm<ChapterValues>({
        resolver: zodResolver(chapterSchema),
        defaultValues: { title: "" },
    });

    const submitChapter = ({ title }: ChapterValues) =>
        addChapter
            .mutateAsync(title)
            .then(() => reset())
            .catch((error: Error) => showToast(`Chưa thêm được chương. ${error.message}`, "error"));

    return (
        <form noValidate onSubmit={handleSubmit(submitChapter)} className="flex flex-col gap-1.5">
            <div className="flex gap-2">
                <Input
                    aria-label="Tên chương mới"
                    placeholder="Tên chương mới"
                    autoComplete="off"
                    aria-invalid={errors.title ? true : undefined}
                    className="h-10 text-base md:text-base"
                    {...register("title")}
                />
                <Button type="submit" size="lg" variant="outline" disabled={addChapter.isPending}>
                    <Plus aria-hidden="true" />
                    Thêm chương
                </Button>
            </div>
            {errors.title ? (
                <p className="text-sm text-destructive">{errors.title.message}</p>
            ) : null}
        </form>
    );
}
