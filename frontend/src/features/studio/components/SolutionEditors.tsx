import { LazySqlEditor } from "@/components/shared/LazySqlEditor";
import { type Dialect, DIALECT_LABELS } from "@/lib/dialect";
import type { CompletionSchema } from "@/lib/sql-completion";

import { ANSWER_DIALECTS, REQUIRED_DIALECT } from "../answer-draft";

interface SolutionEditorsProps {
    sqlByDialect: Record<Dialect, string>;
    enabledDialects: Dialect[];
    schema: CompletionSchema;
    disabled: boolean;
    error: string | undefined;
    onSqlChange: (dialect: Dialect, sql: string) => void;
    onToggleDialect: (dialect: Dialect, isEnabled: boolean) => void;
}

const OPTIONAL_DIALECTS = ANSWER_DIALECTS.filter((dialect) => dialect !== REQUIRED_DIALECT);

export function SolutionEditors({
    sqlByDialect,
    enabledDialects,
    schema,
    disabled,
    error,
    onSqlChange,
    onToggleDialect,
}: SolutionEditorsProps) {
    return (
        <fieldset className="flex flex-col gap-3">
            <legend className="text-sm font-semibold">Đáp án chuẩn</legend>
            <p className="text-[13px] text-muted-foreground">
                Bắt buộc có đáp án PostgreSQL. Thêm MySQL hoặc SQLite nếu bài hỗ trợ các dialect đó,
                đáp án mọi dialect phải cho cùng kết quả.
            </p>
            <div className="flex flex-wrap gap-4">
                {OPTIONAL_DIALECTS.map((dialect) => (
                    <label key={dialect} className="flex items-center gap-2 text-sm">
                        <input
                            type="checkbox"
                            checked={enabledDialects.includes(dialect)}
                            disabled={disabled}
                            onChange={(event) => onToggleDialect(dialect, event.target.checked)}
                        />
                        {`Thêm đáp án ${DIALECT_LABELS[dialect]}`}
                    </label>
                ))}
            </div>
            {enabledDialects.map((dialect) => (
                <div key={dialect} className="flex flex-col gap-1.5">
                    <p aria-hidden="true" className="text-sm font-medium">
                        {DIALECT_LABELS[dialect]}
                    </p>
                    <div className="h-48 overflow-hidden rounded-md border">
                        <LazySqlEditor
                            value={sqlByDialect[dialect]}
                            onChange={(sql) => onSqlChange(dialect, sql)}
                            ariaLabel={`Đáp án ${DIALECT_LABELS[dialect]}`}
                            readOnly={disabled}
                            schema={schema}
                        />
                    </div>
                </div>
            ))}
            {error ? <p className="text-[13px] text-destructive">{error}</p> : null}
        </fieldset>
    );
}
