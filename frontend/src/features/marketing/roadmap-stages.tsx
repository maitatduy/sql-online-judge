import { SqlKeyword } from "@/components/shared/SqlKeyword";

export interface RoadmapStage {
    tag: string;
    keyword: string;
    title: string;
    summary: string;
    subtopics: string[];
    fileName: string;
    lineCount: number;
    columns: string[];
    rows: string[][];
    runtime: string;
    sql: React.ReactNode;
}

function Fn({ children }: { children: React.ReactNode }) {
    return <span className="text-syntax-function">{children}</span>;
}

function Num({ children }: { children: React.ReactNode }) {
    return <span className="text-syntax-number">{children}</span>;
}

function Str({ children }: { children: React.ReactNode }) {
    return <span className="text-syntax-string">{children}</span>;
}

export const ROADMAP_STAGES: RoadmapStage[] = [
    {
        tag: "select-where",
        keyword: "SELECT",
        title: "Truy vấn cơ bản",
        summary: "Chọn cột, lọc dòng và sắp xếp kết quả.",
        subtopics: ["SELECT", "WHERE", "ORDER BY", "LIMIT"],
        fileName: "nhan_vien_ky_thuat.sql",
        lineCount: 4,
        columns: ["name", "salary"],
        rows: [
            ["Nguyễn Văn An", "48000000"],
            ["Hoàng Quốc Bảo", "45000000"],
        ],
        runtime: "9 ms",
        sql: (
            <>
                <SqlKeyword>SELECT</SqlKeyword> name, salary{"\n"}
                <SqlKeyword>FROM</SqlKeyword> employees{"\n"}
                <SqlKeyword>WHERE</SqlKeyword> department = <Str>&apos;Kỹ thuật&apos;</Str>
                {"\n"}
                <SqlKeyword>ORDER BY</SqlKeyword> salary <SqlKeyword>DESC</SqlKeyword>;
            </>
        ),
    },
    {
        tag: "join",
        keyword: "JOIN",
        title: "Nối bảng",
        summary: "Ghép dữ liệu từ nhiều bảng theo khoá.",
        subtopics: ["INNER JOIN", "LEFT JOIN", "SELF JOIN", "CROSS JOIN"],
        fileName: "nhan_vien_phong_ban.sql",
        lineCount: 3,
        columns: ["name", "department"],
        rows: [
            ["Trần Thị Bình", "Kinh doanh"],
            ["Nguyễn Văn An", "Kỹ thuật"],
        ],
        runtime: "14 ms",
        sql: (
            <>
                <SqlKeyword>SELECT</SqlKeyword> e.name, d.name <SqlKeyword>AS</SqlKeyword>{" "}
                department{"\n"}
                <SqlKeyword>FROM</SqlKeyword> employees e{"\n"}
                <SqlKeyword>JOIN</SqlKeyword> departments d <SqlKeyword>ON</SqlKeyword> d.id =
                e.department_id;
            </>
        ),
    },
    {
        tag: "group-by",
        keyword: "GROUP BY",
        title: "Gom nhóm",
        summary: "Đếm, cộng, lấy trung bình theo từng nhóm.",
        subtopics: ["COUNT, SUM, AVG", "GROUP BY", "HAVING"],
        fileName: "tong_luong_phong_ban.sql",
        lineCount: 4,
        columns: ["department_id", "total"],
        rows: [
            ["1", "93000000"],
            ["2", "64000000"],
        ],
        runtime: "11 ms",
        sql: (
            <>
                <SqlKeyword>SELECT</SqlKeyword> department_id, <Fn>SUM</Fn>(salary){" "}
                <SqlKeyword>AS</SqlKeyword> total{"\n"}
                <SqlKeyword>FROM</SqlKeyword> employees{"\n"}
                <SqlKeyword>GROUP BY</SqlKeyword> department_id{"\n"}
                <SqlKeyword>HAVING</SqlKeyword> <Fn>COUNT</Fn>(*) {">"} <Num>1</Num>;
            </>
        ),
    },
    {
        tag: "subquery",
        keyword: "SUBQUERY",
        title: "Truy vấn lồng",
        summary: "Dùng kết quả của một truy vấn làm điều kiện cho truy vấn khác.",
        subtopics: ["IN", "EXISTS", "Subquery tương quan"],
        fileName: "tren_trung_binh.sql",
        lineCount: 3,
        columns: ["name", "salary"],
        rows: [["Nguyễn Văn An", "48000000"]],
        runtime: "16 ms",
        sql: (
            <>
                <SqlKeyword>SELECT</SqlKeyword> name, salary <SqlKeyword>FROM</SqlKeyword> employees
                {"\n"}
                <SqlKeyword>WHERE</SqlKeyword> salary {">"} ({"\n"}
                {"  "}
                <SqlKeyword>SELECT</SqlKeyword> <Fn>AVG</Fn>(salary) <SqlKeyword>FROM</SqlKeyword>{" "}
                employees);
            </>
        ),
    },
    {
        tag: "cte",
        keyword: "WITH",
        title: "CTE",
        summary: "Đặt tên cho bước trung gian để truy vấn dài dễ đọc.",
        subtopics: ["WITH", "CTE đệ quy"],
        fileName: "phong_ban_dong_nguoi.sql",
        lineCount: 5,
        columns: ["department_id", "headcount"],
        rows: [["1", "3"]],
        runtime: "13 ms",
        sql: (
            <>
                <SqlKeyword>WITH</SqlKeyword> sizes <SqlKeyword>AS</SqlKeyword> ({"\n"}
                {"  "}
                <SqlKeyword>SELECT</SqlKeyword> department_id, <Fn>COUNT</Fn>(*){" "}
                <SqlKeyword>AS</SqlKeyword> headcount{"\n"}
                {"  "}
                <SqlKeyword>FROM</SqlKeyword> employees <SqlKeyword>GROUP BY</SqlKeyword>{" "}
                department_id){"\n"}
                <SqlKeyword>SELECT</SqlKeyword> * <SqlKeyword>FROM</SqlKeyword> sizes{"\n"}
                <SqlKeyword>WHERE</SqlKeyword> headcount {">="} <Num>3</Num>;
            </>
        ),
    },
    {
        tag: "window-function",
        keyword: "OVER()",
        title: "Window function",
        summary: "Xếp hạng, cộng dồn, so với dòng trước mà không mất dòng nào.",
        subtopics: ["ROW_NUMBER, RANK", "PARTITION BY", "LAG, LEAD"],
        fileName: "xep_hang_luong.sql",
        lineCount: 3,
        columns: ["name", "rk"],
        rows: [
            ["Nguyễn Văn An", "1"],
            ["Hoàng Quốc Bảo", "2"],
        ],
        runtime: "18 ms",
        sql: (
            <>
                <SqlKeyword>SELECT</SqlKeyword> name,{"\n"}
                {"  "}
                <Fn>RANK</Fn>() <SqlKeyword>OVER</SqlKeyword> (<SqlKeyword>ORDER BY</SqlKeyword>{" "}
                salary <SqlKeyword>DESC</SqlKeyword>) <SqlKeyword>AS</SqlKeyword> rk{"\n"}
                <SqlKeyword>FROM</SqlKeyword> employees;
            </>
        ),
    },
];
