"use client";

import { useFormContext, useWatch } from "react-hook-form";

import { LessonMarkdown } from "@/components/shared/LessonMarkdown";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { formatNumber } from "@/lib/format";

import { type LessonValues, MAX_LESSON_BODY_LENGTH } from "../schemas";

export function LessonBodyField() {
    const {
        register,
        control,
        formState: { errors },
    } = useFormContext<LessonValues>();
    const body = useWatch({ control, name: "body" });
    const error = errors.body?.message;

    return (
        <div className="flex flex-col gap-1.5">
            <Tabs defaultValue="write" className="overflow-hidden rounded-md border">
                <TabsList>
                    <TabsTrigger value="write">Soạn</TabsTrigger>
                    <TabsTrigger value="preview">Xem trước</TabsTrigger>
                </TabsList>
                <TabsContent value="write" forceMount className="data-[state=inactive]:hidden">
                    <textarea
                        aria-label="Nội dung"
                        spellCheck={false}
                        placeholder="Viết bài giảng bằng Markdown"
                        aria-invalid={error ? true : undefined}
                        aria-describedby={error ? "lesson-body-error" : "lesson-body-length"}
                        className="block min-h-[480px] w-full resize-y bg-transparent p-4 text-base leading-relaxed outline-none disabled:cursor-not-allowed disabled:opacity-60"
                        {...register("body")}
                    />
                </TabsContent>
                <TabsContent value="preview" className="min-h-[480px] p-4">
                    {body.trim() ? (
                        <LessonMarkdown>{body}</LessonMarkdown>
                    ) : (
                        <p className="text-muted-foreground">Chưa có nội dung để xem trước.</p>
                    )}
                </TabsContent>
            </Tabs>
            <div className="flex justify-between gap-3 text-sm">
                {error ? (
                    <p id="lesson-body-error" className="text-destructive">
                        {error}
                    </p>
                ) : (
                    <span />
                )}
                <p id="lesson-body-length" className="text-muted-foreground">
                    {formatNumber(body.length)} / {formatNumber(MAX_LESSON_BODY_LENGTH)} ký tự
                </p>
            </div>
        </div>
    );
}
