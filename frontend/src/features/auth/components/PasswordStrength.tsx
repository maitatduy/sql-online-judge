import { Check } from "lucide-react";

import { cn } from "@/lib/utils";

import {
    getPasswordLevel,
    getPasswordRules,
    getPasswordScore,
    MAX_PASSWORD_SCORE,
    type PasswordLevel,
} from "../password-strength";

interface PasswordStrengthProps {
    password: string;
}

const BAR_LEVELS = Array.from({ length: MAX_PASSWORD_SCORE }, (_, index) => index + 1);

const LEVEL_LABELS: Record<PasswordLevel, string> = {
    EMPTY: "Chưa nhập",
    WEAK: "Yếu",
    FAIR: "Trung bình",
    STRONG: "Mạnh",
};

const LEVEL_TEXT_CLASSES: Record<PasswordLevel, string> = {
    EMPTY: "text-muted-foreground",
    WEAK: "text-password-weak",
    FAIR: "text-password-fair",
    STRONG: "text-password-strong",
};

const LEVEL_BAR_CLASSES: Record<PasswordLevel, string> = {
    EMPTY: "bg-border",
    WEAK: "bg-password-weak",
    FAIR: "bg-password-fair",
    STRONG: "bg-password-strong",
};

export function PasswordStrength({ password }: PasswordStrengthProps) {
    const score = getPasswordScore(password);
    const level = getPasswordLevel(password);
    const rules = getPasswordRules(password);

    return (
        <div className="flex flex-col gap-2.5">
            <div className="flex items-center gap-3">
                <div
                    role="meter"
                    aria-label="Độ mạnh mật khẩu"
                    aria-valuemin={0}
                    aria-valuemax={MAX_PASSWORD_SCORE}
                    aria-valuenow={score}
                    aria-valuetext={LEVEL_LABELS[level]}
                    className="flex flex-1 gap-1.5"
                >
                    {BAR_LEVELS.map((barLevel) => (
                        <span
                            key={barLevel}
                            className={cn(
                                "h-1.5 flex-1 rounded-full transition-colors",
                                barLevel <= score ? LEVEL_BAR_CLASSES[level] : "bg-border",
                            )}
                        />
                    ))}
                </div>
                <p
                    aria-live="polite"
                    className={cn(
                        "min-w-20 text-right text-sm font-semibold",
                        LEVEL_TEXT_CLASSES[level],
                    )}
                >
                    {LEVEL_LABELS[level]}
                </p>
            </div>
            <ul aria-label="Tiêu chí mật khẩu" className="grid grid-cols-2 gap-x-4 gap-y-1">
                {rules.map((rule) => (
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
                                rule.isMet ? "text-password-strong" : "text-border",
                            )}
                        />
                        <span>
                            {rule.label}
                            <span className="sr-only">{rule.isMet ? ": đạt" : ": chưa đạt"}</span>
                        </span>
                    </li>
                ))}
            </ul>
        </div>
    );
}
