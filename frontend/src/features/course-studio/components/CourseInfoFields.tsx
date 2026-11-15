"use client";

import { useFormContext } from "react-hook-form";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Input } from "@/components/ui/input";
import { DIFFICULTIES, DIFFICULTY_LABELS } from "@/lib/difficulty";

import type { CourseInfoValues } from "../schemas";
import { CourseFormField } from "./CourseFormField";

const TEXTAREA_CLASS =
    "min-h-20 rounded-md border border-input bg-transparent px-3 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50";

interface CourseInfoFieldsProps {
    idPrefix: string;
    slugField?: React.ReactNode;
}

export function CourseInfoFields({ idPrefix, slugField }: CourseInfoFieldsProps) {
    const {
        register,
        formState: { errors },
    } = useFormContext<CourseInfoValues>();
    return (
        <>
            <CourseFormField
                id={`${idPrefix}-title`}
                label="Tên khoá"
                error={errors.title?.message}
            >
                {(props) => (
                    <Input
                        autoComplete="off"
                        className="h-10 text-base md:text-base"
                        {...props}
                        {...register("title")}
                    />
                )}
            </CourseFormField>
            {slugField}
            <CourseFormField
                id={`${idPrefix}-summary`}
                label="Tóm tắt"
                error={errors.summary?.message}
            >
                {(props) => (
                    <textarea className={TEXTAREA_CLASS} {...props} {...register("summary")} />
                )}
            </CourseFormField>
            <CourseFormField
                id={`${idPrefix}-difficulty`}
                label="Độ khó"
                error={errors.difficulty?.message}
            >
                {(props) => (
                    <NativeSelect
                        className="h-10 text-base sm:w-60"
                        {...props}
                        {...register("difficulty")}
                    >
                        {DIFFICULTIES.map((difficulty) => (
                            <option key={difficulty} value={difficulty}>
                                {DIFFICULTY_LABELS[difficulty]}
                            </option>
                        ))}
                    </NativeSelect>
                )}
            </CourseFormField>
        </>
    );
}
