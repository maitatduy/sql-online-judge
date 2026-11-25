import { Check } from "lucide-react";

import { cn } from "@/lib/utils";

import { getPasswordRules } from "../password-strength";

interface PasswordChecklistProps {
    password: string;
}

export function PasswordChecklist({ password }: PasswordChecklistProps) {
    return (
        <ul aria-label="Tiêu chí mật khẩu" className="grid grid-cols-2 gap-x-4 gap-y-1">
            {getPasswordRules(password).map((rule) => (
                <li
                    key={rule.label}
                    className={cn(
                        "flex items-center gap-1.5 text-[13px]",
                        rule.isMet ? "text-foreground" : "text-muted-foreground",
                    )}
                >
                    <Check
                        aria-hidden="true"
                        className={cn(
                            "size-3.5 shrink-0",
                            rule.isMet ? "text-password-rule-met" : "text-border",
                        )}
                    />
                    <span>
                        {rule.label}
                        <span className="sr-only">{rule.isMet ? ": đạt" : ": chưa đạt"}</span>
                    </span>
                </li>
            ))}
        </ul>
    );
}
