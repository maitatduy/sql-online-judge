import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { DialogClose } from "@/components/ui/dialog";

import { useProblemBankMutation } from "../../hooks/useProblemBankMutation";
import { problemBankApi } from "../../problem-bank/api";
import type { BankProblem } from "../../problem-bank/types";
import { ActionDialog } from "../users/ActionDialog";

export type ConfirmableAction = "hide" | "restore" | "delete";

interface ActionCopy {
    title: string;
    description: string;
    confirmLabel: string;
    done: string;
}

const COPY: Record<ConfirmableAction, ActionCopy> = {
    hide: {
        title: "Ẩn bài này?",
        description: "Người học không thấy và không nộp được bài nữa. Bài nộp cũ vẫn giữ.",
        confirmLabel: "Ẩn bài",
        done: "Đã ẩn bài.",
    },
    restore: {
        title: "Hiện lại bài này?",
        description: "Bài trở về phạm vi hiển thị trước khi bị ẩn.",
        confirmLabel: "Hiện lại",
        done: "Đã hiện lại bài.",
    },
    delete: {
        title: "Xoá bài này?",
        description: "Bài biến khỏi mọi danh sách. Bài nộp cũ vẫn giữ để đối soát.",
        confirmLabel: "Xoá bài",
        done: "Đã xoá bài.",
    },
};

const ACTIONS: Record<ConfirmableAction, (problemId: string) => Promise<void>> = {
    hide: problemBankApi.hide,
    restore: problemBankApi.restore,
    delete: problemBankApi.remove,
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
            .then(() => {
                showToast(copy.done);
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
                    variant={action === "restore" ? "default" : "destructive"}
                    disabled={mutation.isPending}
                    onClick={() => void confirm()}
                >
                    {copy.confirmLabel}
                </Button>
            </div>
        </ActionDialog>
    );
}
