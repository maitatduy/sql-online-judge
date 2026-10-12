import { cn } from "@/lib/utils";

import { getPasswordScore, MAX_PASSWORD_SCORE, MIN_PASSWORD_LENGTH } from "../password-strength";

interface PasswordStrengthProps {
    password: string;
}

const BAR_LEVELS = Array.from({ length: MAX_PASSWORD_SCORE }, (_, index) => index + 1);

export function PasswordStrength({ password }: PasswordStrengthProps) {
    const score = getPasswordScore(password);

    return (
        <div className="flex items-center gap-3">
            <div
                role="meter"
                aria-label="Độ mạnh mật khẩu"
                aria-valuemin={0}
                aria-valuemax={MAX_PASSWORD_SCORE}
                aria-valuenow={score}
                className="flex flex-1 gap-1"
            >
                {BAR_LEVELS.map((level) => (
                    <span
                        key={level}
                        className={cn(
                            "h-1 flex-1 rounded-full",
                            level <= score ? "bg-verdict-ac" : "bg-border",
                        )}
                    />
                ))}
            </div>
            <p className="text-xs text-muted-foreground">Ít nhất {MIN_PASSWORD_LENGTH} ký tự</p>
        </div>
    );
}
