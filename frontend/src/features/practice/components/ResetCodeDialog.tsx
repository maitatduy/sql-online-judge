"use client";

import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog";

interface ResetCodeDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onConfirm: () => void;
}

export function ResetCodeDialog({ open, onOpenChange, onConfirm }: ResetCodeDialogProps) {
    return (
        <AlertDialog open={open} onOpenChange={onOpenChange}>
            <AlertDialogContent className="dark">
                <AlertDialogTitle>Đặt lại mã?</AlertDialogTitle>
                <AlertDialogDescription>
                    Mã bạn đang viết sẽ bị thay bằng mã khởi đầu của bài. Bản nháp đã lưu cũng bị
                    xoá.
                </AlertDialogDescription>
                <AlertDialogFooter>
                    <AlertDialogCancel>Huỷ</AlertDialogCancel>
                    <AlertDialogAction onClick={onConfirm}>Đặt lại</AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
}
