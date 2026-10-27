"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Mail, User } from "lucide-react";
import { useRouter } from "next/navigation";
import { useForm, useWatch } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import { useApiFormErrors } from "../hooks/useApiFormErrors";
import { useRegister } from "../hooks/useRegister";
import { useUsernameAvailability } from "../hooks/useUsernameAvailability";
import { savePendingNextPath, savePendingVerificationEmail } from "../pending-verification";
import { registerSchema, type RegisterFormValues } from "../schemas";

import { FormAlert } from "./FormAlert";
import { PasswordField } from "./PasswordField";
import { PasswordStrength } from "./PasswordStrength";
import { TextField } from "./TextField";
import { UsernameAvailability } from "./UsernameAvailability";

const API_FIELDS = ["username", "email", "password"] as const;
const TERMS_ERROR_ID = "register-terms-error";

interface RegisterFormProps {
    nextPath: string;
}

export function RegisterForm({ nextPath }: RegisterFormProps) {
    const router = useRouter();
    const registerAccount = useRegister();
    const {
        register,
        handleSubmit,
        setError,
        control,
        formState: { errors },
    } = useForm<RegisterFormValues>({
        resolver: zodResolver(registerSchema),
        defaultValues: { username: "", email: "", password: "", acceptedTerms: false },
    });
    const password = useWatch({ control, name: "password" });
    const username = useWatch({ control, name: "username" });
    const usernameAvailability = useUsernameAvailability(username);
    const { formError, clearFormError, handleApiError } = useApiFormErrors(setError, API_FIELDS);
    const isBusy = registerAccount.isPending || registerAccount.isSuccess;
    const termsError = errors.acceptedTerms?.message;

    const submitRegistration = (values: RegisterFormValues) => {
        clearFormError();
        registerAccount.mutate(values, {
            onSuccess: ({ email }) => {
                savePendingVerificationEmail(email);
                savePendingNextPath(nextPath);
                router.push(ROUTES.verifyEmail);
            },
            onError: handleApiError,
        });
    };

    return (
        <form
            noValidate
            onSubmit={handleSubmit(submitRegistration)}
            className="flex flex-col gap-4"
        >
            {formError && <FormAlert message={formError} />}
            <TextField
                id="register-username"
                label="Tên người dùng"
                icon={User}
                autoComplete="username"
                autoCapitalize="none"
                spellCheck={false}
                error={errors.username?.message}
                hint={<UsernameAvailability status={usernameAvailability} />}
                {...register("username")}
            />
            <TextField
                id="register-email"
                label="Email"
                icon={Mail}
                type="email"
                autoComplete="email"
                error={errors.email?.message}
                {...register("email")}
            />
            <div className="flex flex-col gap-2.5">
                <PasswordField
                    id="register-password"
                    label="Mật khẩu"
                    autoComplete="new-password"
                    error={errors.password?.message}
                    {...register("password")}
                />
                <PasswordStrength password={password} />
            </div>
            <div>
                <label className="flex min-h-11 items-center gap-2.5 text-[13px] leading-normal md:min-h-0 md:items-start">
                    <input
                        type="checkbox"
                        aria-invalid={termsError ? true : undefined}
                        aria-describedby={termsError ? TERMS_ERROR_ID : undefined}
                        className="mt-px size-4.5 shrink-0 accent-brand"
                        {...register("acceptedTerms")}
                    />
                    Tôi đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư
                </label>
                {termsError && (
                    <p id={TERMS_ERROR_ID} className="mt-1.5 text-xs text-verdict-wa">
                        {termsError}
                    </p>
                )}
            </div>
            <Button type="submit" size="lg" disabled={isBusy} className="h-12.5 text-[15px]">
                {isBusy ? "Đang tạo tài khoản…" : "Tạo tài khoản"}
                {!isBusy && <ArrowRight aria-hidden="true" />}
            </Button>
        </form>
    );
}
