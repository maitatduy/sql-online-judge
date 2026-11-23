import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { ApiRequestError } from "@/lib/api-client";

import { useProblemBankMutation } from "../../hooks/useProblemBankMutation";
import { problemBankApi } from "../../problem-bank/api";
import { transferOwnershipSchema, type TransferOwnershipValues } from "../../problem-bank/schemas";
import type { BankProblem } from "../../problem-bank/types";
import { ActionDialog } from "../users/ActionDialog";
import { DialogActions } from "../users/DialogActions";
import { TextInputField } from "../users/TextInputField";

const RECIPIENT_ERRORS = new Set(["USER_NOT_FOUND", "NEW_OWNER_NOT_SETTER"]);

interface TransferOwnershipDialogProps {
    problem: BankProblem;
    onClose: () => void;
}

export function TransferOwnershipDialog({ problem, onClose }: TransferOwnershipDialogProps) {
    const showToast = useToast();
    const transfer = useProblemBankMutation(problemBankApi.transferOwnership);
    const {
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<TransferOwnershipValues>({
        resolver: zodResolver(transferOwnershipSchema),
        defaultValues: { username: "" },
    });

    const submitTransfer = ({ username }: TransferOwnershipValues) =>
        transfer
            .mutateAsync({ problemId: problem.id, username })
            .then(() => {
                showToast(`Đã chuyển bài cho ${username}.`);
                onClose();
            })
            .catch((error: Error) => {
                if (error instanceof ApiRequestError && RECIPIENT_ERRORS.has(error.code)) {
                    setError("username", { message: error.message });
                    return;
                }
                showToast(`Chưa chuyển được. ${error.message}`, "error");
            });

    return (
        <ActionDialog
            title="Chuyển chủ sở hữu"
            description={`${problem.title}: hiện thuộc ${problem.authorUsername ?? "người ra đề không rõ"}.`}
            onClose={onClose}
        >
            <form
                noValidate
                onSubmit={handleSubmit(submitTransfer)}
                className="flex flex-col gap-4"
            >
                <TextInputField
                    label="Username người nhận"
                    placeholder="Người ra đề đang hoạt động"
                    error={errors.username?.message}
                    registration={register("username")}
                />
                <DialogActions submitLabel="Chuyển" isSubmitDisabled={transfer.isPending} />
            </form>
        </ActionDialog>
    );
}
