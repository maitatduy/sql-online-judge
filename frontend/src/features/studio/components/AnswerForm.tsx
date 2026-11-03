"use client";

import { useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { ApiRequestError } from "@/lib/api-client";
import type { Dialect } from "@/lib/dialect";
import type { CompletionSchema } from "@/lib/sql-completion";
import { useUnsavedChangesWarning } from "@/lib/useUnsavedChangesWarning";

import {
    ANSWER_DIALECTS,
    type AnswerDraft,
    draftFrom,
    isDraftComplete,
    requestFrom,
} from "../answer-draft";
import { useSaveProblemAnswer } from "../hooks/useProblemAnswer";
import type { ProblemAnswer } from "../types";
import { CompareSettings } from "./CompareSettings";
import { SaveBar } from "./SaveBar";
import { SolutionEditors } from "./SolutionEditors";
import { TestcasePicker } from "./TestcasePicker";

interface AnswerFormProps {
    problemId: string;
    answer: ProblemAnswer;
    schema: CompletionSchema;
    disabled: boolean;
}

function fieldErrorOf(error: Error | null, fieldPrefix: string): string | undefined {
    return error instanceof ApiRequestError
        ? error.errors.find((fieldError) => fieldError.field.startsWith(fieldPrefix))?.message
        : undefined;
}

export function AnswerForm({ problemId, answer, schema, disabled }: AnswerFormProps) {
    const showToast = useToast();
    const [draft, setDraft] = useState<AnswerDraft>(() => draftFrom(answer));
    const save = useSaveProblemAnswer(problemId, (error) => {
        if (!(error instanceof ApiRequestError) || error.errors.length === 0) {
            showToast(`Chưa lưu được đáp án. ${error.message}`, "error");
        }
    });
    const hasChanges =
        JSON.stringify(requestFrom(draft)) !== JSON.stringify(requestFrom(draftFrom(answer)));
    useUnsavedChangesWarning(hasChanges);

    function update(change: Partial<AnswerDraft>) {
        setDraft((current) => ({ ...current, ...change }));
    }

    function handleToggleDialect(dialect: Dialect, isEnabled: boolean) {
        setDraft((current) => ({
            ...current,
            enabledDialects: ANSWER_DIALECTS.filter((candidate) =>
                candidate === dialect ? isEnabled : current.enabledDialects.includes(candidate),
            ),
        }));
    }

    return (
        <div className="flex flex-col gap-6">
            <SolutionEditors
                sqlByDialect={draft.sqlByDialect}
                enabledDialects={draft.enabledDialects}
                schema={schema}
                disabled={disabled}
                error={fieldErrorOf(save.error, "solutions")}
                onSqlChange={(dialect, sql) =>
                    update({ sqlByDialect: { ...draft.sqlByDialect, [dialect]: sql } })
                }
                onToggleDialect={handleToggleDialect}
            />
            <TestcasePicker
                variants={answer.variants}
                selectedIds={draft.testcaseIds}
                disabled={disabled}
                error={fieldErrorOf(save.error, "testcaseVariantIds")}
                onChange={(testcaseIds) => update({ testcaseIds })}
            />
            <CompareSettings
                requiresOrder={draft.requiresOrder}
                compareColumnNames={draft.compareColumnNames}
                floatTolerance={draft.floatTolerance}
                disabled={disabled}
                error={fieldErrorOf(save.error, "floatTolerance")}
                onRequiresOrderChange={(requiresOrder) => update({ requiresOrder })}
                onCompareColumnNamesChange={(compareColumnNames) => update({ compareColumnNames })}
                onFloatToleranceChange={(floatTolerance) => update({ floatTolerance })}
            />
            {disabled ? null : (
                <div className="border-t pt-4">
                    <SaveBar
                        hasChanges={hasChanges}
                        canSave={hasChanges && isDraftComplete(draft)}
                        isSaving={save.isSaving}
                        savedAt={save.savedAt}
                        onSave={() => save.save(requestFrom(draft))}
                    />
                </div>
            )}
        </div>
    );
}
