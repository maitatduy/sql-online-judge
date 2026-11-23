"use client";

import { useId, useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogTitle,
} from "@/components/ui/dialog";

import { communityApi } from "../api";
import { useCommunityMutation } from "../hooks/useCommunity";
import { REPORT_REASON_LABELS, REPORT_REASONS } from "../labels";
import type { ReportReason, ReportTarget } from "../types";

interface ReportDialogProps {
    targetType: ReportTarget;
    targetId: string;
    onClose: () => void;
}

export function ReportDialog({ targetType, targetId, onClose }: ReportDialogProps) {
    const showToast = useToast();
    const detailId = useId();
    const report = useCommunityMutation(communityApi.report);
    const [reason, setReason] = useState<ReportReason>("SPAM");
    const [detail, setDetail] = useState("");

    const submit = () =>
        report
            .mutateAsync({ targetType, targetId, reason, detail: detail.trim() || undefined })
            .then(() => {
                showToast("Đã gửi báo cáo. Cảm ơn bạn.");
                onClose();
            })
            .catch((error: Error) => showToast(error.message, "error"));

    return (
        <Dialog open onOpenChange={(isOpen) => (isOpen ? null : onClose())}>
            <DialogContent className="max-w-[440px]">
                <div>
                    <DialogTitle>Báo cáo vi phạm</DialogTitle>
                    <DialogDescription>
                        Kiểm duyệt viên sẽ xem xét báo cáo của bạn.
                    </DialogDescription>
                </div>
                <fieldset className="flex flex-col gap-2">
                    <legend className="sr-only">Lý do</legend>
                    {REPORT_REASONS.map((option) => (
                        <label key={option} className="flex items-center gap-2.5 text-[15px]">
                            <input
                                type="radio"
                                name="report-reason"
                                value={option}
                                checked={reason === option}
                                onChange={() => setReason(option)}
                                className="size-4 accent-brand"
                            />
                            {REPORT_REASON_LABELS[option]}
                        </label>
                    ))}
                </fieldset>
                <label htmlFor={detailId} className="sr-only">
                    Chi tiết
                </label>
                <textarea
                    id={detailId}
                    rows={3}
                    maxLength={500}
                    placeholder="Chi tiết (không bắt buộc)"
                    value={detail}
                    onChange={(event) => setDetail(event.target.value)}
                    className="rounded-md border bg-transparent px-3 py-2 text-[15px]"
                />
                <div className="flex justify-end gap-2">
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Huỷ
                        </Button>
                    </DialogClose>
                    <Button type="button" disabled={report.isPending} onClick={() => void submit()}>
                        Gửi báo cáo
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    );
}
