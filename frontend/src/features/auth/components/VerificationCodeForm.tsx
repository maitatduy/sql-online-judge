"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { KeyRound } from "lucide-react";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";

import { useApiFormErrors } from "../hooks/useApiFormErrors";
import { useConfirmEmail } from "../hooks/useConfirmEmail";
import { verificationCodeSchema, type VerificationCodeFormValues } from "../schemas";

import { FormAlert } from "./FormAlert";
import { TextField } from "./TextField";

interface VerificationCodeFormProps {
    email: string;
    onVerified: () => void;
}

const INVALID_CODE_ERROR = "TOKEN_INVALID_OR_EXPIRED";
const API_FIELDS = ["code"] as const;

export function VerificationCodeForm({ email, onVerified }: VerificationCodeFormProps) {
    const confirmEmail = useConfirmEmail();
    const {
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<VerificationCodeFormValues>({
        resolver: zodResolver(verificationCodeSchema),
        defaultValues: { code: "" },
    });
    const { formError, clearFormError, handleApiError } = useApiFormErrors(setError, API_FIELDS);

    const handleConfirm = ({ code }: VerificationCodeFormValues) => {
        clearFormError();
        confirmEmail.mutate(
            { email, code },
            {
                onSuccess: onVerified,
                onError: (error) => {
                    if (error instanceof ApiRequestError && error.code === INVALID_CODE_ERROR) {
                        setError("code", { message: error.message });
                        return;
                    }
                    handleApiError(error);
                },
            },
        );
    };

    return (
        <form noValidate onSubmit={handleSubmit(handleConfirm)} className="flex flex-col gap-4">
            {formError && <FormAlert message={formError} />}
            <TextField
                id="verification-code"
                label="Mã xác thực"
                icon={KeyRound}
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                className="font-mono text-lg tracking-[0.4em] md:text-lg"
                error={errors.code?.message}
                {...register("code")}
            />
            <Button
                type="submit"
                size="lg"
                disabled={confirmEmail.isPending}
                className="h-12.5 text-[15px]"
            >
                {confirmEmail.isPending ? "Đang xác thực…" : "Xác thực"}
            </Button>
        </form>
    );
}
