import { EditorPreview } from "@/components/shared/EditorPreview";
import { SqlKeyword } from "@/components/shared/SqlKeyword";

const RESULT_COLUMNS = ["department", "name", "salary"];

const RESULT_ROWS = [
    ["Kinh doanh", "Trần Thị Bình", "32000000"],
    ["Kỹ thuật", "Nguyễn Văn An", "48000000"],
    ["Kỹ thuật", "Hoàng Quốc Bảo", "48000000"],
];

export function HeroEditorPreview() {
    return (
        <EditorPreview
            label="Ví dụ một truy vấn được chấm đúng"
            fileName="top_salary_per_department.sql"
            verdictDetail="48 ms"
            lineCount={6}
            columns={RESULT_COLUMNS}
            rows={RESULT_ROWS}
            className="flex-[1_1_520px]"
        >
            <SqlKeyword>WITH</SqlKeyword> r <SqlKeyword>AS</SqlKeyword> ({"\n"}
            {"  "}
            <SqlKeyword>SELECT</SqlKeyword> *, <span className="text-syntax-function">RANK</span>(){" "}
            <SqlKeyword>OVER</SqlKeyword> (<SqlKeyword>PARTITION BY</SqlKeyword> department_id{"\n"}
            {"         "}
            <SqlKeyword>ORDER BY</SqlKeyword> salary <SqlKeyword>DESC</SqlKeyword>){" "}
            <SqlKeyword>AS</SqlKeyword> rk{"\n"}
            {"  "}
            <SqlKeyword>FROM</SqlKeyword> employees){"\n"}
            <SqlKeyword>SELECT</SqlKeyword> department, name, salary <SqlKeyword>FROM</SqlKeyword> r
            {"\n"}
            <SqlKeyword>WHERE</SqlKeyword> rk = <span className="text-syntax-number">1</span>{" "}
            <SqlKeyword>ORDER BY</SqlKeyword> department;
        </EditorPreview>
    );
}
