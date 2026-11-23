import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { DialogClose } from "@/components/ui/dialog";
import { formatNumber } from "@/lib/format";

import { useProblemBankMutation } from "../../hooks/useProblemBankMutation";
import { problemBankApi } from "../../problem-bank/api";
import type { BankProblem, RejudgeBatch } from "../../problem-bank/types";
import { ActionDialog } from "../users/ActionDialog";

export type ConfirmableAction = "hide" | "restore" | "delete" | "rejudge";

interface ActionCopy {
    title: string;
    description: string;
    confirmLabel: string;
    done: (result: unknown) => string;
}

const COPY: Record<ConfirmableAction, ActionCopy> = {
    hide: {
        title: "Ẩn bài này?",
        description: "Người học không thấy và không nộp được bài nữa. Bài nộp cũ vẫn giữ.",
        confirmLabel: "Ẩn bài",
        done: () => "Đã ẩn bài.",
    },
    restore: {
        title: "Hiện lại bài này?",
        description: "Bài trở về phạm vi hiển thị trước khi bị ẩn.",
        confirmLabel: "Hiện lại",
        done: () => "Đã hiện lại bài.",
    },
    delete: {
        title: "Xoá bài này?",
        description: "Bài biến khỏi mọi danh sách. Bài nộp cũ vẫn giữ để đối soát.",
        confirmLabel: "Xoá bài",
        done: () => "Đã xoá bài.",
    },
    rejudge: {
        title: "Chấm lại bài này?",
        description:
            "Mọi bài nộp luyện tập được chấm lại theo testcase hiện tại. Bài nộp trong contest giữ nguyên.",
        confirmLabel: "Chấm lại",
        done: (result) =>
            `Đang chấm lại ${formatNumber((result as RejudgeBatch).totalCount)} bài nộp.`,
    },
};

const ACTIONS: Record<ConfirmableAction, (problemId: string) => Promise<unknown>> = {
    hide: problemBankApi.hide,
    restore: problemBankApi.restore,
    delete: problemBankApi.remove,
    rejudge: problemBankApi.rejudge,
};

interface ProblemActionConfirmDialogProps {
    action: ConfirmableAction;
    problem: BankProblem;
    onClose: () => void;
}

export function ProblemActionConfirmDialog({
    action,
    problem,
    onClose,
}: ProblemActionConfirmDialogProps) {
    const showToast = useToast();
    const mutation = useProblemBankMutation(ACTIONS[action]);
    const copy = COPY[action];

    const confirm = () =>
        mutation
            .mutateAsync(problem.id)
            .then((result) => {
                showToast(copy.done(result));
                onClose();
            })
            .catch((error: Error) => showToast(error.message, "error"));

    return (
        <ActionDialog
            title={copy.title}
            description={`${problem.title}. ${copy.description}`}
            onClose={onClose}
        >
            <div className="flex justify-end gap-2">
                <DialogClose asChild>
                    <Button type="button" variant="outline">
                        Huỷ
                    </Button>
                </DialogClose>
                <Button
                    type="button"
                    variant={action === "hide" || action === "delete" ? "destructive" : "default"}
                    disabled={mutation.isPending}
                    onClick={() => void confirm()}
                >
                    {copy.confirmLabel}
                </Button>
            </div>
        </ActionDialog>
    );
}
