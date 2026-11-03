import { ArrowDown, ArrowUp } from "lucide-react";

import { IconAction } from "@/components/shared/IconAction";

import type { AnswerVariant } from "../types";

interface TestcasePickerProps {
    variants: AnswerVariant[];
    selectedIds: string[];
    disabled: boolean;
    error: string | undefined;
    onChange: (selectedIds: string[]) => void;
}

function VisibilityBadge({ isPublic }: { isPublic: boolean }) {
    return (
        <span className="rounded-full bg-surface-2 px-2 py-0.5 text-xs text-muted-foreground">
            {isPublic ? "Công khai" : "Ẩn"}
        </span>
    );
}

function moved(ids: string[], index: number, offset: number): string[] {
    const next = [...ids];
    [next[index], next[index + offset]] = [next[index + offset], next[index]];
    return next;
}

export function TestcasePicker({
    variants,
    selectedIds,
    disabled,
    error,
    onChange,
}: TestcasePickerProps) {
    const selected = selectedIds.flatMap((id) => variants.filter((variant) => variant.id === id));

    function toggle(variantId: string, isChecked: boolean) {
        onChange(
            isChecked
                ? [...selectedIds, variantId]
                : selectedIds.filter((selectedId) => selectedId !== variantId),
        );
    }

    return (
        <fieldset className="flex flex-col gap-3">
            <legend className="text-sm font-semibold">Testcase</legend>
            <p className="text-[13px] text-muted-foreground">
                Bộ công khai là testcase mẫu, người làm bài xem được kết quả. Bộ ẩn là testcase ẩn,
                chỉ báo đúng hay sai.
            </p>
            <ul className="flex flex-col gap-2">
                {variants.map((variant) => (
                    <li key={variant.id} className="flex items-center gap-2 text-sm">
                        <label className="flex items-center gap-2">
                            <input
                                type="checkbox"
                                checked={selectedIds.includes(variant.id)}
                                disabled={disabled}
                                onChange={(event) => toggle(variant.id, event.target.checked)}
                            />
                            {variant.name}
                        </label>
                        <VisibilityBadge isPublic={variant.isPublic} />
                    </li>
                ))}
            </ul>
            {selected.length > 0 ? (
                <ol
                    aria-label="Testcase đã chọn"
                    className="flex flex-col gap-1 rounded-md border p-2"
                >
                    {selected.map((variant, index) => (
                        <li key={variant.id} className="flex items-center gap-2 text-sm">
                            <span className="w-6 text-muted-foreground">{index + 1}.</span>
                            <span>{variant.name}</span>
                            <VisibilityBadge isPublic={variant.isPublic} />
                            {disabled ? null : (
                                <span className="ml-auto flex gap-1">
                                    <IconAction
                                        label={`Đưa ${variant.name} lên trước`}
                                        tooltip="Lên trước"
                                        icon={ArrowUp}
                                        disabled={index === 0}
                                        onClick={() => onChange(moved(selectedIds, index, -1))}
                                    />
                                    <IconAction
                                        label={`Đưa ${variant.name} xuống sau`}
                                        tooltip="Xuống sau"
                                        icon={ArrowDown}
                                        disabled={index === selected.length - 1}
                                        onClick={() => onChange(moved(selectedIds, index, 1))}
                                    />
                                </span>
                            )}
                        </li>
                    ))}
                </ol>
            ) : null}
            {error ? <p className="text-[13px] text-destructive">{error}</p> : null}
        </fieldset>
    );
}
