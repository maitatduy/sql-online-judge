import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import { isToleranceValid } from "../answer-draft";

interface CompareSettingsProps {
    requiresOrder: boolean;
    compareColumnNames: boolean;
    floatTolerance: string;
    disabled: boolean;
    error: string | undefined;
    onRequiresOrderChange: (requiresOrder: boolean) => void;
    onCompareColumnNamesChange: (compareColumnNames: boolean) => void;
    onFloatToleranceChange: (floatTolerance: string) => void;
}

export function CompareSettings({
    requiresOrder,
    compareColumnNames,
    floatTolerance,
    disabled,
    error,
    onRequiresOrderChange,
    onCompareColumnNamesChange,
    onFloatToleranceChange,
}: CompareSettingsProps) {
    const toleranceError = isToleranceValid(floatTolerance) ? error : "Nhập số từ 0 đến 1";

    return (
        <fieldset className="flex flex-col gap-3">
            <legend className="text-sm font-semibold">Cài đặt so khớp</legend>
            <label className="flex items-center gap-2 text-sm">
                <input
                    type="checkbox"
                    checked={requiresOrder}
                    disabled={disabled}
                    onChange={(event) => onRequiresOrderChange(event.target.checked)}
                />
                So khớp đúng thứ tự dòng
            </label>
            <label className="flex items-center gap-2 text-sm">
                <input
                    type="checkbox"
                    checked={compareColumnNames}
                    disabled={disabled}
                    onChange={(event) => onCompareColumnNamesChange(event.target.checked)}
                />
                So khớp tên cột
            </label>
            <div className="flex flex-col gap-1.5">
                <Label htmlFor="answer-float-tolerance">Sai số khi so số thực</Label>
                <Input
                    id="answer-float-tolerance"
                    type="number"
                    step="any"
                    min={0}
                    max={1}
                    className="w-40"
                    value={floatTolerance}
                    disabled={disabled}
                    aria-invalid={toleranceError ? true : undefined}
                    onChange={(event) => onFloatToleranceChange(event.target.value)}
                />
                {toleranceError ? (
                    <p className="text-[13px] text-destructive">{toleranceError}</p>
                ) : null}
            </div>
        </fieldset>
    );
}
