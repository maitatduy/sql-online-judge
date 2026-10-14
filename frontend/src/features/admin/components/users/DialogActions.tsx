import { Button } from "@/components/ui/button";
import { DialogClose } from "@/components/ui/dialog";

interface DialogActionsProps {
    submitLabel: string;
    isSubmitDisabled: boolean;
    submitVariant?: "default" | "destructive";
}

export function DialogActions({
    submitLabel,
    isSubmitDisabled,
    submitVariant = "default",
}: DialogActionsProps) {
    return (
        <div className="flex justify-end gap-2">
            <DialogClose asChild>
                <Button type="button" variant="outline">
                    Huỷ
                </Button>
            </DialogClose>
            <Button type="submit" variant={submitVariant} disabled={isSubmitDisabled}>
                {submitLabel}
            </Button>
        </div>
    );
}
