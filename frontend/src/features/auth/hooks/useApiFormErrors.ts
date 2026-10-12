import { useState } from "react";
import type { FieldValues, Path, UseFormSetError } from "react-hook-form";

import { getFieldErrors, getFormErrorMessage } from "../form-errors";

export function useApiFormErrors<TValues extends FieldValues>(
    setError: UseFormSetError<TValues>,
    fields: readonly Path<TValues>[],
) {
    const [formError, setFormError] = useState<string | null>(null);

    const clearFormError = () => setFormError(null);

    const handleApiError = (error: unknown) => {
        const fieldErrors = getFieldErrors(error, fields);
        if (fieldErrors.length === 0) {
            setFormError(getFormErrorMessage(error));
            return;
        }
        fieldErrors.forEach(({ field, message }) => setError(field, { message }));
    };

    return { formError, clearFormError, handleApiError };
}
