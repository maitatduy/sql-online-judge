import { ApiRequestError } from "@/lib/api-client";

export interface FieldMessage<TField extends string> {
    field: TField;
    message: string;
}

export function fieldErrorsOf<TField extends string>(
    error: unknown,
    fields: readonly TField[],
): FieldMessage<TField>[] {
    if (!(error instanceof ApiRequestError)) {
        return [];
    }
    return error.errors.filter((fieldError): fieldError is FieldMessage<TField> =>
        fields.includes(fieldError.field as TField),
    );
}
