import { Eye, EyeOff, Pencil, RotateCw, Trash2, UserRoundPen } from "lucide-react";

import { IconAction } from "@/components/shared/IconAction";

import type { BankProblem } from "../../problem-bank/types";

export type ProblemRowAction = "edit" | "hide" | "restore" | "transfer" | "rejudge" | "delete";

interface ProblemRowActionsProps {
    problem: BankProblem;
    onAction: (action: ProblemRowAction, problem: BankProblem) => void;
}

export function ProblemRowActions({ problem, onAction }: ProblemRowActionsProps) {
    const isHidden = problem.visibility === "HIDDEN";
    const name = `bài ${problem.number ?? problem.title}`;

    return (
        <div className="flex justify-end gap-1">
            <IconAction
                label={`Sửa nhanh ${name}`}
                tooltip="Sửa nhanh"
                icon={Pencil}
                onClick={() => onAction("edit", problem)}
            />
            <IconAction
                label={`${isHidden ? "Hiện lại" : "Ẩn"} ${name}`}
                tooltip={isHidden ? "Hiện lại" : "Ẩn"}
                icon={isHidden ? Eye : EyeOff}
                onClick={() => onAction(isHidden ? "restore" : "hide", problem)}
            />
            <IconAction
                label={`Chuyển chủ sở hữu ${name}`}
                tooltip="Chuyển chủ sở hữu"
                icon={UserRoundPen}
                onClick={() => onAction("transfer", problem)}
            />
            <IconAction
                label={`Chấm lại ${name}`}
                tooltip="Chấm lại"
                icon={RotateCw}
                onClick={() => onAction("rejudge", problem)}
            />
            <IconAction
                label={`Xoá ${name}`}
                tooltip="Xoá"
                icon={Trash2}
                onClick={() => onAction("delete", problem)}
                className="hover:bg-destructive/10 hover:text-destructive"
            />
        </div>
    );
}
