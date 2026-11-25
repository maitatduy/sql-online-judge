import { SchemaTableCard } from "./SchemaTableCard";

interface SchemaIllustrationProps {
    sampleLearnerName: string;
}

export function SchemaIllustration({ sampleLearnerName }: SchemaIllustrationProps) {
    return (
        <div aria-hidden="true" className="relative hidden h-[270px] w-[560px] max-w-full xl:block">
            <svg viewBox="0 0 560 270" fill="none" className="absolute inset-0 size-full">
                <path
                    d="M236 92C300 92 290 186 318 186"
                    strokeWidth="1.5"
                    strokeDasharray="5 5"
                    className="stroke-brand"
                />
                <circle cx="236" cy="92" r="4" className="fill-brand" />
                <circle cx="318" cy="186" r="4" className="fill-brand" />
                <text x="302" y="124" fontSize="11" className="fill-muted-foreground">
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
        </div>
    );
}
