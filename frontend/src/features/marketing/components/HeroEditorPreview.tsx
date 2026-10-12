import { VerdictBadge } from "@/components/shared/VerdictBadge";

const RESULT_COLUMNS = ["department", "name", "salary"];

const RESULT_ROWS = [
    ["Kinh doanh", "Trần Thị Bình", "32000000"],
    ["Kỹ thuật", "Nguyễn Văn An", "48000000"],
    ["Kỹ thuật", "Hoàng Quốc Bảo", "48000000"],
];

const LINE_NUMBERS = [1, 2, 3, 4, 5, 6];

function Keyword({ children }: { children: React.ReactNode }) {
    return <span className="font-semibold text-syntax-keyword">{children}</span>;
}

export function HeroEditorPreview() {
    return (
        <figure
            aria-label="Ví dụ một truy vấn được chấm đúng"
            className="dark min-w-0 flex-[1_1_520px] overflow-hidden rounded-xl border bg-background font-mono text-foreground shadow-2xl"
        >
            <div className="flex h-10 items-center gap-2 border-b bg-surface px-3.5">
                <span aria-hidden="true" className="flex gap-2">
                    <span className="size-2 rounded-full bg-muted-foreground/40" />
                    <span className="size-2 rounded-full bg-muted-foreground/40" />
                    <span className="size-2 rounded-full bg-muted-foreground/40" />
                </span>
                <span className="ml-2.5 font-mono text-xs text-muted-foreground">bai-184.sql</span>
                <VerdictBadge verdict="AC" detail="48 ms" variant="solid" className="ml-auto" />
            </div>
            <div className="flex gap-3.5 overflow-x-auto py-4 font-mono text-[13.5px] leading-[22px]">
                <div
                    aria-hidden="true"
                    className="w-9 shrink-0 text-right text-muted-foreground/60 select-none"
                >
                    {LINE_NUMBERS.map((lineNumber) => (
                        <div key={lineNumber}>{lineNumber}</div>
                    ))}
                </div>
                <pre className="pr-4">
                    <code>
                        <Keyword>WITH</Keyword> r <Keyword>AS</Keyword> ({"\n"}
                        {"  "}
                        <Keyword>SELECT</Keyword> *,{" "}
                        <span className="text-syntax-function">RANK</span>() <Keyword>OVER</Keyword>{" "}
                        (<Keyword>PARTITION BY</Keyword> department_id{"\n"}
                        {"         "}
                        <Keyword>ORDER BY</Keyword> salary <Keyword>DESC</Keyword>){" "}
                        <Keyword>AS</Keyword> rk{"\n"}
                        {"  "}
                        <Keyword>FROM</Keyword> employees){"\n"}
                        <Keyword>SELECT</Keyword> department, name, salary <Keyword>FROM</Keyword> r
                        {"\n"}
                        <Keyword>WHERE</Keyword> rk = <span className="text-syntax-number">1</span>{" "}
                        <Keyword>ORDER BY</Keyword> department;
                    </code>
                </pre>
            </div>
            <div className="overflow-x-auto border-t bg-surface">
                <table className="w-full border-collapse font-mono text-xs">
                    <caption className="sr-only">Kết quả truy vấn</caption>
                    <thead>
                        <tr>
                            {RESULT_COLUMNS.map((column) => (
                                <th
                                    key={column}
                                    scope="col"
                                    className="border-b px-3 py-2 text-left font-semibold text-muted-foreground"
                                >
                                    {column}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {RESULT_ROWS.map((row) => (
                            <tr key={row.join("|")}>
                                {row.map((cell) => (
                                    <td key={cell} className="border-b px-3 py-2">
                                        {cell}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </figure>
    );
}
