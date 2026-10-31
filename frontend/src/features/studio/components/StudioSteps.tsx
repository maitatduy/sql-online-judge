import Link from "next/link";

const LATER_STEPS = ["Đề bài", "Dataset và schema", "Đáp án và testcase", "Xem trước và xuất bản"];
const STEP_CLASS = "flex items-center gap-2.5 rounded-md px-3 py-2 text-sm";
const NUMBER_CLASS =
    "flex size-6 shrink-0 items-center justify-center rounded-full font-mono text-xs font-bold";

interface StudioStepsProps {
    problemId: string;
}

export function StudioSteps({ problemId }: StudioStepsProps) {
    return (
        <nav aria-label="Các bước soạn bài">
            <ol className="flex flex-col gap-1">
                <li>
                    <Link
                        href={`/studio/problems/${problemId}`}
                        aria-current="step"
                        className={`${STEP_CLASS} bg-brand-soft font-semibold text-brand`}
                    >
                        <span aria-hidden="true" className={`${NUMBER_CLASS} bg-brand text-white`}>
                            1
                        </span>
                        Thông tin
                    </Link>
                </li>
                {LATER_STEPS.map((step, index) => (
                    <li key={step} className={`${STEP_CLASS} text-muted-foreground`}>
                        <span aria-hidden="true" className={`${NUMBER_CLASS} bg-surface-2`}>
                            {index + 2}
                        </span>
                        {step}
                    </li>
                ))}
            </ol>
        </nav>
    );
}
