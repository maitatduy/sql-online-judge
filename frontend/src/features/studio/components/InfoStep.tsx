"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiRequestError } from "@/lib/api-client";
import { DIFFICULTIES, DIFFICULTY_LABELS } from "@/lib/difficulty";

import { useStudioTags } from "../hooks/useStudioProblem";
import { MAX_TAGS, problemInfoSchema, type ProblemInfoValues } from "../schemas";
import type { StudioProblem, UpdateProblemRequest } from "../types";

type InfoField = keyof ProblemInfoValues;

interface InfoStepProps {
    problem: StudioProblem;
    disabled: boolean;
    saveError: Error | null;
    onChange: (change: UpdateProblemRequest) => void;
}

function serverErrorOf(error: Error | null, field: InfoField): string | undefined {
    if (!(error instanceof ApiRequestError)) {
        return undefined;
    }
    if (field === "slug" && error.code === "SLUG_TAKEN") {
        return error.message;
    }
    return error.errors.find((fieldError) => fieldError.field === field)?.message;
}

export function InfoStep({ problem, disabled, saveError, onChange }: InfoStepProps) {
    const { data: tags } = useStudioTags();
    const {
        register,
        trigger,
        getValues,
        setValue,
        watch,
        formState: { errors },
    } = useForm<ProblemInfoValues>({
        resolver: zodResolver(problemInfoSchema),
        defaultValues: {
            title: problem.title,
            slug: problem.slug,
            difficulty: problem.difficulty,
            tagSlugs: problem.tags.map((tag) => tag.slug),
        },
    });
    const selectedTags = watch("tagSlugs");

    async function saveField(field: InfoField) {
        if (await trigger(field)) {
            const value = getValues(field);
            onChange({ [field]: typeof value === "string" ? value.trim() : value });
        }
    }

    function toggleTag(slug: string) {
        const next = selectedTags.includes(slug)
            ? selectedTags.filter((selected) => selected !== slug)
            : [...selectedTags, slug];
        setValue("tagSlugs", next);
        void saveField("tagSlugs");
    }

    function errorOf(field: InfoField): string | undefined {
        return errors[field]?.message ?? serverErrorOf(saveError, field);
    }

    function describedBy(field: InfoField) {
        return errorOf(field)
            ? { "aria-invalid": true, "aria-describedby": `problem-${field}-error` }
            : {};
    }

    function fieldError(field: InfoField) {
        const message = errorOf(field);
        return message ? (
            <p id={`problem-${field}-error`} className="text-[13px] text-destructive">
                {message}
            </p>
        ) : null;
    }

    return (
        <section aria-labelledby="info-step-title" className="rounded-lg border bg-card p-6">
            <h2 id="info-step-title" className="text-base font-bold">
                Thông tin
            </h2>
            <form noValidate onSubmit={(event) => event.preventDefault()}>
                <fieldset disabled={disabled} className="mt-4 flex flex-col gap-4">
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="problem-title">Tên bài</Label>
                        <Input
                            id="problem-title"
                            autoComplete="off"
                            {...describedBy("title")}
                            {...register("title", { onChange: () => void saveField("title") })}
                        />
                        {fieldError("title")}
                    </div>
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="problem-slug">Slug</Label>
                        <Input
                            id="problem-slug"
                            autoComplete="off"
                            className="font-mono"
                            {...describedBy("slug")}
                            {...register("slug", { onChange: () => void saveField("slug") })}
                        />
                        {fieldError("slug")}
                    </div>
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="problem-difficulty">Độ khó</Label>
                        <NativeSelect
                            id="problem-difficulty"
                            className="w-48"
                            {...register("difficulty", {
                                onChange: () => void saveField("difficulty"),
                            })}
                        >
                            {DIFFICULTIES.map((difficulty) => (
                                <option key={difficulty} value={difficulty}>
                                    {DIFFICULTY_LABELS[difficulty]}
                                </option>
                            ))}
                        </NativeSelect>
                    </div>
                    <fieldset
                        aria-describedby="problem-tags-hint"
                        className="flex flex-col gap-1.5"
                    >
                        <legend className="text-sm font-medium">Chủ đề</legend>
                        <p id="problem-tags-hint" className="text-[13px] text-muted-foreground">
                            Chọn tối đa {MAX_TAGS} chủ đề.
                        </p>
                        <div className="mt-1 flex flex-wrap gap-2">
                            {tags?.items.map((tag) => {
                                const isSelected = selectedTags.includes(tag.slug);
                                return (
                                    <label
                                        key={tag.slug}
                                        className="flex min-h-9 items-center gap-2 rounded-md border px-3 text-sm has-checked:border-brand has-checked:bg-brand-soft"
                                    >
                                        <input
                                            type="checkbox"
                                            checked={isSelected}
                                            disabled={
                                                !isSelected && selectedTags.length >= MAX_TAGS
                                            }
                                            onChange={() => toggleTag(tag.slug)}
                                        />
                                        {tag.name}
                                    </label>
                                );
                            })}
                        </div>
                        {fieldError("tagSlugs")}
                    </fieldset>
                </fieldset>
            </form>
        </section>
    );
}
