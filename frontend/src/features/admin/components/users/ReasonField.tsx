import { useId } from "react";
import type { UseFormRegisterReturn } from "react-hook-form";

interface ReasonFieldProps {
    label: string;
    registration: UseFormRegisterReturn;
    error?: string;
    placeholder?: string;
    rows?: number;
}

export function ReasonField({
    label,
    registration,
    error,
    placeholder,
    rows = 3,
}: ReasonFieldProps) {
    const fieldId = useId();
    const errorId = useId();

    return (
        <div>
            <label htmlFor={fieldId} className="mb-1.5 block text-[13px] font-semibold">
                {label}
            </label>
            <textarea
                id={fieldId}
                rows={rows}
                placeholder={placeholder}
                aria-invalid={error ? true : undefined}
                aria-describedby={error ? errorId : undefined}
                className="w-full rounded-md border border-input bg-transparent px-2.5 py-2 text-sm outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 aria-invalid:border-destructive"
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
