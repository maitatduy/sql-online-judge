import { useId } from "react";

interface ControlProps {
    id: string;
    "aria-invalid": true | undefined;
    "aria-describedby": string | undefined;
}

interface SettingsFieldProps {
    label: string;
    error?: string;
    hint?: string;
    children: (control: ControlProps) => React.ReactNode;
}

export const FIELD_CONTROL_CLASS =
    "w-full rounded-md border border-input bg-transparent px-3 py-2 text-[15px] outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 aria-invalid:border-destructive";

export function SettingsField({ label, error, hint, children }: SettingsFieldProps) {
    const id = useId();
    const hintId = `${id}-hint`;
    const errorId = `${id}-error`;
    const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(" ");

    return (
        <div>
            <label htmlFor={id} className="mb-1.5 block text-sm font-semibold">
                {label}
            </label>
            {children({
                id,
                "aria-invalid": error ? true : undefined,
                "aria-describedby": describedBy || undefined,
            })}
            {hint ? (
                <p id={hintId} className="mt-1 text-[13px] text-muted-foreground">
                    {hint}
                </p>
            ) : null}
            {error ? (
                <p id={errorId} className="mt-1 text-sm text-destructive">
                    {error}
                </p>
            ) : null}
        </div>
    );
}
