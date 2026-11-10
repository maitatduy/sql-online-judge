import Link from "next/link";

import { CircleAlert, CircleCheck } from "lucide-react";

import type { PublishCondition } from "../publish-conditions";

interface PublishConditionsProps {
    problemId: string;
    conditions: PublishCondition[];
    canEdit: boolean;
}

export function PublishConditions({ problemId, conditions, canEdit }: PublishConditionsProps) {
    return (
        <ul aria-label="Điều kiện gửi duyệt" className="flex flex-col">
            {conditions.map(({ label, isMet, detail, action }) => (
                <li
                    key={label}
                    className="flex items-start gap-3 border-t py-3 first:border-t-0 first:pt-0"
                >
                    {isMet ? (
                        <CircleCheck aria-hidden="true" className="mt-0.5 size-4 text-brand" />
                    ) : (
                        <CircleAlert
                            aria-hidden="true"
                            className="mt-0.5 size-4 text-destructive"
                        />
                    )}
                    <div className="flex min-w-0 flex-1 flex-col">
                        <span className="sr-only">{isMet ? "Đạt" : "Chưa đạt"}</span>
                        <span className="text-sm font-semibold">{label}</span>
                        <span className="text-[13px] text-muted-foreground">{detail}</span>
                    </div>
                    {!isMet && canEdit ? (
                        <Link
                            href={`/studio/problems/${problemId}${action.step}`}
                            className="text-[13px] font-semibold text-brand hover:underline"
                        >
                            {action.label}
                        </Link>
                    ) : null}
                </li>
            ))}
        </ul>
    );
}
