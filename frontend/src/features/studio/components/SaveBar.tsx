import { Button } from "@/components/ui/button";
import { formatTime } from "@/lib/format";

interface SaveBarProps {
    hasChanges: boolean;
    canSave: boolean;
    isSaving: boolean;
    savedAt: string | null;
    onSave: () => void;
}

function statusOf(hasChanges: boolean, isSaving: boolean, savedAt: string | null) {
    if (isSaving) {
        return "Đang lưu…";
    }
    if (hasChanges) {
        return "Có thay đổi chưa lưu";
    }
    return savedAt ? `Đã lưu ${formatTime(savedAt)}` : null;
}

export function SaveBar({ hasChanges, canSave, isSaving, savedAt, onSave }: SaveBarProps) {
    return (
        <div className="flex items-center justify-end gap-3">
            <span role="status" className="text-[13px] text-muted-foreground">
                {statusOf(hasChanges, isSaving, savedAt)}
            </span>
            <Button type="button" disabled={!canSave || isSaving} onClick={onSave}>
                Lưu
            </Button>
        </div>
    );
}
