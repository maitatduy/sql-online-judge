import { useState } from "react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { DialogClose } from "@/components/ui/dialog";
import { DIFFICULTIES, DIFFICULTY_LABELS } from "@/lib/difficulty";
import { formatNumber } from "@/lib/format";

import { useBankTags } from "../../hooks/useProblemBank";
import { useProblemBankMutation } from "../../hooks/useProblemBankMutation";
import { problemBankApi } from "../../problem-bank/api";
import type { BulkUpdateRequest, BulkUpdateResult } from "../../problem-bank/types";
import { ActionDialog } from "../users/ActionDialog";

export type BulkAction = "tag" | "difficulty" | "hide" | "rejudge";

const TITLES: Record<BulkAction, string> = {
    tag: "Gắn chủ đề",
    difficulty: "Đổi độ khó",
    hide: "Ẩn các bài đã chọn?",
    rejudge: "Chấm lại các bài đã chọn?",
};

function resultMessage({ succeeded, failed }: BulkUpdateResult): string {
    const done = `Đã cập nhật ${formatNumber(succeeded.length)} bài`;
    return failed.length > 0 ? `${done}, ${formatNumber(failed.length)} bài lỗi.` : `${done}.`;
}

interface BulkProblemDialogProps {
    action: BulkAction;
    problemIds: string[];
    onClose: () => void;
    onDone: () => void;
}

export function BulkProblemDialog({ action, problemIds, onClose, onDone }: BulkProblemDialogProps) {
    const showToast = useToast();
    const { data: tags } = useBankTags();
    const bulkUpdate = useProblemBankMutation(problemBankApi.bulkUpdate);
    const rejudgeMany = useProblemBankMutation(problemBankApi.rejudgeMany);
    const isPending = bulkUpdate.isPending || rejudgeMany.isPending;
    const [value, setValue] = useState(action === "difficulty" ? DIFFICULTIES[0] : "");

    function requestOf(): BulkUpdateRequest {
        if (action === "tag") {
            return { problemIds, addTagSlugs: [value] };
        }
        return action === "difficulty"
            ? { problemIds, difficulty: value }
            : { problemIds, hide: true };
    }

    const submit = () =>
        (action === "rejudge"
            ? rejudgeMany.mutateAsync(problemIds)
            : bulkUpdate.mutateAsync(requestOf())
        )
            .then((result) => {
                showToast(resultMessage(result), result.failed.length > 0 ? "error" : undefined);
                onDone();
            })
            .catch((error: Error) => showToast(error.message, "error"));

    return (
        <ActionDialog
            title={TITLES[action]}
            description={`Áp dụng cho ${formatNumber(problemIds.length)} bài đã chọn.`}
            onClose={onClose}
        >
            {action === "tag" ? (
                <NativeSelect
                    aria-label="Chủ đề"
                    value={value}
                    onChange={(event) => setValue(event.target.value)}
                >
                    <option value="">Chọn chủ đề</option>
                    {(tags ?? []).map((tag) => (
                        <option key={tag.slug} value={tag.slug}>
                            {tag.name}
                        </option>
                    ))}
                </NativeSelect>
            ) : null}
            {action === "difficulty" ? (
                <NativeSelect
                    aria-label="Độ khó"
                    value={value}
                    onChange={(event) => setValue(event.target.value)}
                >
                    {DIFFICULTIES.map((difficulty) => (
                        <option key={difficulty} value={difficulty}>
                            {DIFFICULTY_LABELS[difficulty]}
                        </option>
                    ))}
                </NativeSelect>
            ) : null}
            <div className="flex justify-end gap-2">
                <DialogClose asChild>
                    <Button type="button" variant="outline">
                        Huỷ
                    </Button>
                </DialogClose>
                <Button
                    type="button"
                    variant={action === "hide" ? "destructive" : "default"}
                    disabled={isPending || (action === "tag" && !value)}
                    onClick={() => void submit()}
                >
                    {action === "hide" ? "Ẩn bài" : action === "rejudge" ? "Chấm lại" : "Áp dụng"}
                </Button>
            </div>
        </ActionDialog>
    );
}
