import { EditorPreview } from "@/components/shared/EditorPreview";
import { SqlKeyword } from "@/components/shared/SqlKeyword";

const RESULT_COLUMNS = ["name", "salary"];

const RESULT_ROWS = [
    ["Nguyễn Văn An", "48000000"],
    ["Hoàng Quốc Bảo", "48000000"],
    ["Trần Thị Bình", "32000000"],
];

export function CtaEditorPreview() {
    return (
        <EditorPreview
            label="Ví dụ câu truy vấn đầu tiên"
            fileName="first_query.sql"
            verdictDetail="12 ms"
            lineCount={4}
            columns={RESULT_COLUMNS}
            rows={RESULT_ROWS}
            size="lg"
        >
            <SqlKeyword>SELECT</SqlKeyword> name, salary{"\n"}
            <SqlKeyword>FROM</SqlKeyword> employees{"\n"}
            <SqlKeyword>ORDER BY</SqlKeyword> salary <SqlKeyword>DESC</SqlKeyword>
            {"\n"}
            <SqlKeyword>LIMIT</SqlKeyword> <span className="text-syntax-number">3</span>;
        </EditorPreview>
    );
}
