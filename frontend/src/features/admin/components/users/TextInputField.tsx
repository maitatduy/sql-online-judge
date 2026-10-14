import { useId } from "react";
import type { UseFormRegisterReturn } from "react-hook-form";

import { Input } from "@/components/ui/input";

interface TextInputFieldProps {
    label: string;
    registration: UseFormRegisterReturn;
    error?: string;
    placeholder?: string;
    type?: "text" | "email";
    isReadOnly?: boolean;
}

export function TextInputField({
    label,
    registration,
    error,
    placeholder,
    type = "text",
    isReadOnly = false,
}: TextInputFieldProps) {
    const fieldId = useId();
    const errorId = useId();

    return (
        <div>
            <label htmlFor={fieldId} className="mb-1.5 block text-[13px] font-semibold">
                {label}
            </label>
            <Input
                id={fieldId}
                type={type}
                placeholder={placeholder}
                readOnly={isReadOnly}
                aria-invalid={error ? true : undefined}
                aria-describedby={error ? errorId : undefined}
                className="shadow-none read-only:bg-surface read-only:text-muted-foreground"
                {...registration}
            />
            {error ? (
                <p id={errorId} className="mt-1 text-[13px] text-destructive">
                    {error}
                </p>
            ) : null}
        </div>
    );
}
