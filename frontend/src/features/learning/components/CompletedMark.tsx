import { CircleCheck } from "lucide-react";

export function CompletedMark() {
    return (
        <span className="ml-auto inline-flex shrink-0 text-brand">
            <CircleCheck aria-hidden="true" className="size-4" />
            <span className="sr-only">Đã hoàn thành</span>
        </span>
    );
}
