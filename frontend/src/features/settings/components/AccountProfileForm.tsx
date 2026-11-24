"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import { fieldErrorsOf } from "../field-errors";
import { useUpdateAccountProfile } from "../hooks/useAccountSettings";
import {
    ACCOUNT_PROFILE_FIELDS,
    accountProfileSchema,
    type AccountProfileValues,
} from "../schemas";
import type { AccountProfile } from "../types";

import { FIELD_CONTROL_CLASS, SettingsField } from "./SettingsField";

interface AccountProfileFormProps {
    profile: AccountProfile;
}

function valuesOf(profile: AccountProfile): AccountProfileValues {
    return {
        displayName: profile.displayName,
        bio: profile.bio ?? "",
        school: profile.school ?? "",
        location: profile.location ?? "",
        websiteUrl: profile.websiteUrl ?? "",
    };
}

export function AccountProfileForm({ profile }: AccountProfileFormProps) {
    const showToast = useToast();
    const update = useUpdateAccountProfile();
    const {
        register,
        handleSubmit,
        setError,
        reset,
        formState: { errors, isDirty },
    } = useForm<AccountProfileValues>({
        resolver: zodResolver(accountProfileSchema),
        defaultValues: valuesOf(profile),
    });

    const handleSave = async (values: AccountProfileValues) => {
        try {
            const saved = await update.mutateAsync(values);
            reset(valuesOf(saved));
            showToast("Đã lưu hồ sơ");
        } catch (error) {
            const fieldErrors = fieldErrorsOf(error, ACCOUNT_PROFILE_FIELDS);
            fieldErrors.forEach(({ field, message }) => setError(field, { message }));
            if (fieldErrors.length === 0) {
                showToast((error as Error).message, "error");
            }
        }
    };

    return (
        <section aria-labelledby="profile-settings" className="rounded-lg border bg-card p-6">
            <h2 id="profile-settings" className="mb-4 text-base font-bold">
                Hồ sơ công khai
            </h2>
            <form noValidate onSubmit={handleSubmit(handleSave)} className="grid gap-4">
                <SettingsField label="Tên hiển thị" error={errors.displayName?.message}>
                    {(control) => (
                        <input
                            {...control}
                            className={FIELD_CONTROL_CLASS}
                            {...register("displayName")}
                        />
                    )}
                </SettingsField>
                <SettingsField
                    label="Giới thiệu"
                    error={errors.bio?.message}
                    hint="Tối đa 300 ký tự"
                >
                    {(control) => (
                        <textarea
                            {...control}
                            rows={3}
                            className={FIELD_CONTROL_CLASS}
                            {...register("bio")}
                        />
                    )}
                </SettingsField>
                <div className="grid gap-4 sm:grid-cols-2">
                    <SettingsField label="Trường, công ty" error={errors.school?.message}>
                        {(control) => (
                            <input
                                {...control}
                                className={FIELD_CONTROL_CLASS}
                                {...register("school")}
                            />
                        )}
                    </SettingsField>
                    <SettingsField label="Nơi sống" error={errors.location?.message}>
                        {(control) => (
                            <input
                                {...control}
                                className={FIELD_CONTROL_CLASS}
                                {...register("location")}
                            />
                        )}
                    </SettingsField>
                </div>
                <SettingsField label="Website" error={errors.websiteUrl?.message}>
                    {(control) => (
                        <input
                            {...control}
                            type="url"
                            placeholder="https://github.com/ten-ban"
                            className={FIELD_CONTROL_CLASS}
                            {...register("websiteUrl")}
                        />
                    )}
                </SettingsField>
                <div>
                    <Button type="submit" disabled={!isDirty || update.isPending}>
                        Lưu thay đổi
                    </Button>
                </div>
            </form>
        </section>
    );
}
