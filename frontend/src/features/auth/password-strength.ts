export const MIN_PASSWORD_LENGTH = 8;
const LONG_PASSWORD_LENGTH = 12;

export interface PasswordRule {
    label: string;
    isMet: boolean;
}

export function getPasswordRules(password: string): PasswordRule[] {
    return [
        {
            label: `Ít nhất ${MIN_PASSWORD_LENGTH} ký tự`,
            isMet: password.length >= MIN_PASSWORD_LENGTH,
        },
        {
            label: "Có cả chữ và số",
            isMet: /\p{L}/u.test(password) && /\d/.test(password),
        },
        { label: "Có ký tự đặc biệt", isMet: /[^\p{L}\d\s]/u.test(password) },
        {
            label: `Từ ${LONG_PASSWORD_LENGTH} ký tự trở lên`,
            isMet: password.length >= LONG_PASSWORD_LENGTH,
        },
    ];
}
