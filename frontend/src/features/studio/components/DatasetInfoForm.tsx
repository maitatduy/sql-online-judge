"use client";

import { useState } from "react";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiRequestError } from "@/lib/api-client";

import type { StudioDataset, UpdateDatasetRequest } from "../types";

const MAX_NAME_LENGTH = 120;
const MAX_DESCRIPTION_LENGTH = 500;

interface DatasetInfoFormProps {
    dataset: StudioDataset;
    saveError: Error | null;
    onChange: (change: UpdateDatasetRequest) => void;
}

function serverErrorOf(error: Error | null, field: keyof UpdateDatasetRequest) {
    return error instanceof ApiRequestError
        ? error.errors.find((fieldError) => fieldError.field === field)?.message
        : undefined;
}

export function DatasetInfoForm({ dataset, saveError, onChange }: DatasetInfoFormProps) {
    const [name, setName] = useState(dataset.name);
    const [description, setDescription] = useState(dataset.description);
    const nameError =
        name.trim() === ""
            ? "Không được để trống"
            : name.length > MAX_NAME_LENGTH
              ? "Tối đa 120 ký tự"
              : serverErrorOf(saveError, "name");
    const descriptionError =
        description.length > MAX_DESCRIPTION_LENGTH
            ? "Tối đa 500 ký tự"
            : serverErrorOf(saveError, "description");

    function changeName(value: string) {
        setName(value);
        if (value.trim() !== "" && value.length <= MAX_NAME_LENGTH) {
            onChange({ name: value });
        }
    }

    function changeDescription(value: string) {
        setDescription(value);
        if (value.length <= MAX_DESCRIPTION_LENGTH) {
            onChange({ description: value });
        }
    }

    return (
        <section
            aria-label="Thông tin database"
            className="grid gap-4 rounded-lg border bg-card p-4 md:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]"
        >
            <div className="flex flex-col gap-1.5">
                <Label htmlFor="database-name">Tên database</Label>
                <Input
                    id="database-name"
                    value={name}
                    autoComplete="off"
                    aria-invalid={nameError ? true : undefined}
                    aria-describedby={nameError ? "database-name-error" : undefined}
                    onChange={(event) => changeName(event.target.value)}
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
                    value={description}
                    autoComplete="off"
                    placeholder="Ví dụ: Phòng ban và nhân viên kèm lương"
                    aria-invalid={descriptionError ? true : undefined}
                    aria-describedby={descriptionError ? "database-description-error" : undefined}
                    onChange={(event) => changeDescription(event.target.value)}
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
