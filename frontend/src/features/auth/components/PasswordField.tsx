"use client";

import { Eye, EyeOff, Lock } from "lucide-react";
import { useState } from "react";

import { TextField, type TextFieldProps } from "./TextField";

type PasswordFieldProps = Omit<TextFieldProps, "type" | "icon" | "trailing">;

export function PasswordField(props: PasswordFieldProps) {
    const [isVisible, setIsVisible] = useState(false);
    const ToggleIcon = isVisible ? EyeOff : Eye;

    return (
        <TextField
            {...props}
            type={isVisible ? "text" : "password"}
            icon={Lock}
            trailing={
                <button
                    type="button"
                    aria-label={isVisible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                    onClick={() => setIsVisible((current) => !current)}
                    className="absolute top-0.5 right-0.5 inline-flex size-11 items-center justify-center rounded-md text-muted-foreground hover:text-foreground"
                >
                    <ToggleIcon aria-hidden="true" className="size-4.5" />
                </button>
            }
        />
    );
}
