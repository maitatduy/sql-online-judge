import { Label } from "@/components/ui/label";

interface CourseFormFieldProps {
    id: string;
    label: string;
    error?: string;
    children: (inputProps: FieldInputProps) => React.ReactNode;
}

export interface FieldInputProps {
    id: string;
    "aria-invalid"?: true;
    "aria-describedby"?: string;
}

export function CourseFormField({ id, label, error, children }: CourseFormFieldProps) {
    const errorId = `${id}-error`;
    return (
        <div className="flex flex-col gap-1.5">
            <Label htmlFor={id} className="text-sm">
                {label}
            </Label>
            {children(error ? { id, "aria-invalid": true, "aria-describedby": errorId } : { id })}
            {error ? (
                <p id={errorId} className="text-sm text-destructive">
                    {error}
                </p>
            ) : null}
        </div>
    );
}
