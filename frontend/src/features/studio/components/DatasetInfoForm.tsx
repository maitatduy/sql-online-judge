"use client";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export interface DatasetInfoValues {
    name: string;
    description: string;
}

interface DatasetInfoFormProps {
    values: DatasetInfoValues;
    errorOf: (field: keyof DatasetInfoValues) => string | undefined;
    onChange: (values: DatasetInfoValues) => void;
}

export function DatasetInfoForm({ values, errorOf, onChange }: DatasetInfoFormProps) {
    const nameError = errorOf("name");
    const descriptionError = errorOf("description");

    return (
        <section
            aria-label="Thông tin database"
            className="grid gap-4 rounded-lg border bg-card p-4 md:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]"
        >
            <div className="flex flex-col gap-1.5">
                <Label htmlFor="database-name">Tên database</Label>
                <Input
                    id="database-name"
                    value={values.name}
                    autoComplete="off"
                    aria-invalid={nameError ? true : undefined}
                    aria-describedby={nameError ? "database-name-error" : undefined}
                    onChange={(event) => onChange({ ...values, name: event.target.value })}
                />
                {nameError ? (
                    <p id="database-name-error" className="text-[13px] text-destructive">
                        {nameError}
                    </p>
                ) : null}
            </div>
            <div className="flex flex-col gap-1.5">
                <Label htmlFor="database-description">Mô tả</Label>
                <Input
                    id="database-description"
                    value={values.description}
                    autoComplete="off"
                    placeholder="Ví dụ: Phòng ban và nhân viên kèm lương"
                    aria-invalid={descriptionError ? true : undefined}
                    aria-describedby={descriptionError ? "database-description-error" : undefined}
                    onChange={(event) => onChange({ ...values, description: event.target.value })}
                />
                {descriptionError ? (
                    <p id="database-description-error" className="text-[13px] text-destructive">
                        {descriptionError}
                    </p>
                ) : null}
            </div>
        </section>
    );
}
