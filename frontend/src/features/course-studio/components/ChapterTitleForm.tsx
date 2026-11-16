"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import { useRenameChapter } from "../hooks/useCourseOutline";
import { chapterSchema, type ChapterValues } from "../schemas";

interface ChapterTitleFormProps {
    chapterId: string;
    title: string;
    onDone: () => void;
}

export function ChapterTitleForm({ chapterId, title, onDone }: ChapterTitleFormProps) {
    const showToast = useToast();
    const renameChapter = useRenameChapter();
    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<ChapterValues>({
        resolver: zodResolver(chapterSchema),
        defaultValues: { title },
    });

    const submitTitle = (values: ChapterValues) =>
        renameChapter
            .mutateAsync({ chapterId, title: values.title })
            .then(onDone)
            .catch((error: Error) => showToast(`Chưa đổi được tên. ${error.message}`, "error"));

    return (
        <form
            noValidate
            onSubmit={handleSubmit(submitTitle)}
            className="flex flex-1 flex-wrap items-center gap-2"
        >
            <Input
                aria-label="Tên chương"
                autoComplete="off"
                autoFocus
                aria-invalid={errors.title ? true : undefined}
                className="h-10 min-w-0 flex-1 text-base md:text-base"
                {...register("title")}
            />
            <Button type="submit" disabled={renameChapter.isPending}>
                Lưu tên
            </Button>
            <Button type="button" variant="ghost" onClick={onDone}>
                Huỷ
            </Button>
            {errors.title ? (
                <p className="w-full text-sm text-destructive">{errors.title.message}</p>
            ) : null}
        </form>
    );
}
