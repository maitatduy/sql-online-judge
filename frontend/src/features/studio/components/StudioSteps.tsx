import Link from "next/link";

import { cn } from "@/lib/utils";

import type { StudioStep } from "../types";

const EDITABLE_STEPS: { step: StudioStep; label: string; path: string }[] = [
    { step: "info", label: "Thông tin", path: "" },
    { step: "statement", label: "Đề bài", path: "/statement" },
];
const LATER_STEPS = ["Dataset và schema", "Đáp án và testcase", "Xem trước và xuất bản"];
const STEP_CLASS = "flex items-center gap-2.5 rounded-md px-3 py-2 text-sm";
const NUMBER_CLASS =
    "flex size-6 shrink-0 items-center justify-center rounded-full font-mono text-xs font-bold";

interface StudioStepsProps {
    problemId: string;
    currentStep: StudioStep;
}

export function StudioSteps({ problemId, currentStep }: StudioStepsProps) {
    return (
        <nav aria-label="Các bước soạn bài">
            <ol className="flex flex-col gap-1">
                {EDITABLE_STEPS.map(({ step, label, path }, index) => {
                    const isCurrent = step === currentStep;
                    return (
                        <li key={step}>
                            <Link
                                href={`/studio/problems/${problemId}${path}`}
                                aria-current={isCurrent ? "step" : undefined}
                                className={cn(
                                    STEP_CLASS,
                                    isCurrent
                                        ? "bg-brand-soft font-semibold text-brand"
                                        : "hover:bg-surface-2",
                                )}
                            >
                                <span
                                    aria-hidden="true"
                                    className={cn(
                                        NUMBER_CLASS,
                                        isCurrent ? "bg-brand text-white" : "bg-surface-2",
                                    )}
                                >
                                    {index + 1}
                                </span>
                                {label}
                            </Link>
                        </li>
                    );
                })}
                {LATER_STEPS.map((label, index) => (
                    <li key={label} className={cn(STEP_CLASS, "text-muted-foreground")}>
                        <span aria-hidden="true" className={cn(NUMBER_CLASS, "bg-surface-2")}>
                            {EDITABLE_STEPS.length + index + 1}
                        </span>
                        {label}
                    </li>
                ))}
            </ol>
        </nav>
    );
}
