"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Mail } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import { useApiFormErrors } from "../hooks/useApiFormErrors";
import { useLogin } from "../hooks/useLogin";
import { loginSchema, type LoginFormValues } from "../schemas";

import { FormAlert } from "./FormAlert";
import { PasswordField } from "./PasswordField";
import { TextField } from "./TextField";

interface LoginFormProps {
    nextPath: string;
}

const API_FIELDS = ["email", "password"] as const;

export function LoginForm({ nextPath }: LoginFormProps) {
    const router = useRouter();
    const login = useLogin();
    const {
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<LoginFormValues>({
        resolver: zodResolver(loginSchema),
        defaultValues: { email: "", password: "", rememberMe: false },
    });
    const { formError, clearFormError, handleApiError } = useApiFormErrors(setError, API_FIELDS);
    const isBusy = login.isPending || login.isSuccess;

    const submitLogin = (values: LoginFormValues) => {
        clearFormError();
        login.mutate(values, {
            onSuccess: () => router.push(nextPath),
            onError: handleApiError,
        });
    };

    return (
        <form noValidate onSubmit={handleSubmit(submitLogin)} className="flex flex-col gap-4">
            {formError && <FormAlert message={formError} />}
            <TextField
                id="login-email"
                label="Email"
                icon={Mail}
                type="email"
                autoComplete="email"
                error={errors.email?.message}
                {...register("email")}
            />
            <PasswordField
                id="login-password"
                label="Mật khẩu"
                autoComplete="current-password"
                error={errors.password?.message}
                labelAction={
                    <Link
                        href={ROUTES.forgotPassword}
                        className="text-[13px] font-semibold text-brand underline-offset-4 hover:underline"
                    >
                        Quên mật khẩu?
                    </Link>
                }
                {...register("password")}
            />
            <label className="flex min-h-11 items-center gap-2.5 text-sm md:min-h-0">
                <input
                    type="checkbox"
                    className="size-4.5 accent-brand"
                    {...register("rememberMe")}
                />
                Ghi nhớ đăng nhập trên thiết bị này
            </label>
            <Button type="submit" size="lg" disabled={isBusy} className="h-12.5 text-[15px]">
                {isBusy ? "Đang đăng nhập…" : "Đăng nhập"}
                {!isBusy && <ArrowRight aria-hidden="true" />}
            </Button>
        </form>
    );
}
