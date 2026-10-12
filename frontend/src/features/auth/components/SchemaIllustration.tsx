import { VerdictBadge } from "@/components/shared/VerdictBadge";

import { SchemaTableCard } from "./SchemaTableCard";

interface SchemaIllustrationProps {
    sampleLearnerName: string;
}

export function SchemaIllustration({ sampleLearnerName }: SchemaIllustrationProps) {
    return (
        <div aria-hidden="true" className="relative hidden h-[330px] w-[560px] max-w-full xl:block">
            <svg viewBox="0 0 560 330" fill="none" className="absolute inset-0 size-full">
                <path
                    d="M236 92C300 92 290 186 318 186"
                    strokeWidth="1.5"
                    strokeDasharray="5 5"
                    className="stroke-brand"
                />
                <circle cx="236" cy="92" r="4" className="fill-brand" />
                <circle cx="318" cy="186" r="4" className="fill-brand" />
                <text x="302" y="124" fontSize="11" className="fill-muted-foreground font-mono">
                    1 - n
                </text>
            </svg>
            <SchemaTableCard
                name="nguoi_hoc"
                className="top-7 left-0"
                columns={[
                    { name: "id", type: "INT", key: "PK" },
                    {
                        name: "ten",
                        type: sampleLearnerName,
                        isHighlighted: true,
                        typeClassName: "text-syntax-function",
                    },
                    { name: "chuoi_ngay", type: "INT" },
                ]}
            />
            <SchemaTableCard
                name="bai_nop"
                className="top-[140px] left-[318px]"
                columns={[
                    { name: "id", type: "INT", key: "PK" },
                    { name: "nguoi_hoc_id", type: "INT", key: "FK" },
                    {
                        name: "ket_qua",
                        type: "'AC'",
                        typeClassName: "font-bold text-verdict-ac",
                    },
                ]}
            />
            <div className="dark absolute top-[218px] left-6 w-[268px] rounded-lg border bg-surface px-3.5 py-3 font-mono text-xs leading-[1.7] text-foreground shadow-2xl">
                <p>
                    <span className="font-bold text-syntax-keyword">SELECT</span> ket_qua{" "}
                    <span className="font-bold text-syntax-keyword">FROM</span> bai_nop
                </p>
                <p>
                    <span className="font-bold text-syntax-keyword">WHERE</span> nguoi_hoc_id ={" "}
                    <span className="text-syntax-string">:ban</span>;
                </p>
                <p className="mt-2 flex items-center gap-2 border-t border-dashed pt-2 text-muted-foreground">
                    <VerdictBadge verdict="AC" variant="solid" />1 dòng · 48 ms
                </p>
            </div>
        </div>
    );
}
