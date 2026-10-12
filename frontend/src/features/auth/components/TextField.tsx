import type { LucideIcon } from "lucide-react";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

export interface TextFieldProps extends React.ComponentProps<"input"> {
    id: string;
    label: string;
    icon: LucideIcon;
    error?: string;
    labelAction?: React.ReactNode;
    trailing?: React.ReactNode;
    hint?: React.ReactNode;
}

export function TextField({
    id,
    label,
    icon: Icon,
    error,
    labelAction,
    trailing,
    hint,
    className,
    ...inputProps
}: TextFieldProps) {
    const errorId = `${id}-error`;

    return (
        <div>
            <div className="mb-1.5 flex items-baseline justify-between">
                <Label htmlFor={id} className="text-[13px] font-semibold">
                    {label}
                </Label>
                {labelAction}
            </div>
            <div className="relative">
                <Icon
                    aria-hidden="true"
                    className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                    id={id}
                    aria-invalid={error ? true : undefined}
                    aria-describedby={error ? errorId : undefined}
                    className={cn(
                        "h-12 rounded-lg border-muted-foreground/70 pl-10.5 text-[15px] md:text-[15px]",
                        trailing && "pr-12",
                        className,
                    )}
                    {...inputProps}
                />
                {trailing}
            </div>
            {error ? (
                <p id={errorId} className="mt-1.5 text-xs text-verdict-wa">
                    {error}
                </p>
            ) : (
                hint
            )}
        </div>
    );
}
