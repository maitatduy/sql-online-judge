import { SqlKeyword } from "@/components/shared/SqlKeyword";

export interface RoadmapStage {
    tag: string;
    keyword: string;
    title: string;
    summary: string;
    theory: string[];
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
        theory: [
            "SELECT chọn cột cần lấy, FROM cho biết lấy từ bảng nào.",
            "WHERE giữ lại những dòng thoả điều kiện, kết hợp được bằng AND, OR, NOT và so sánh với NULL bằng IS NULL.",
            "ORDER BY sắp xếp kết quả tăng (ASC) hoặc giảm (DESC); LIMIT lấy N dòng đầu.",
            "Thứ tự viết là SELECT, FROM, WHERE, ORDER BY nhưng database chạy FROM và WHERE trước, SELECT sau.",
        ],
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
        theory: [
            "JOIN ghép mỗi dòng của bảng này với các dòng khớp điều kiện ON ở bảng kia, thường là khoá chính và khoá ngoại.",
            "INNER JOIN chỉ giữ cặp dòng khớp nhau; LEFT JOIN giữ mọi dòng bên trái, phần bên phải không khớp thành NULL.",
            "SELF JOIN là nối một bảng với chính nó, ví dụ tìm quản lý của từng nhân viên.",
            "Đặt bí danh ngắn cho bảng (e, d) để câu truy vấn gọn và tránh trùng tên cột.",
        ],
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
        theory: [
            "GROUP BY gom các dòng có cùng giá trị vào một nhóm, mỗi nhóm cho ra một dòng kết quả.",
            "Hàm tổng hợp COUNT, SUM, AVG, MIN, MAX tính trên từng nhóm.",
            "Cột trong SELECT phải nằm trong GROUP BY hoặc nằm trong một hàm tổng hợp.",
            "WHERE lọc dòng trước khi gom nhóm, HAVING lọc nhóm sau khi đã tính tổng hợp.",
        ],
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
        theory: [
            "Subquery là một câu SELECT đặt trong ngoặc, dùng được ở WHERE, FROM hoặc SELECT.",
            "Subquery trả một giá trị dùng để so sánh; trả nhiều dòng thì dùng với IN hoặc EXISTS.",
            "Subquery tương quan tham chiếu tới cột của truy vấn ngoài nên chạy lại cho từng dòng.",
            "EXISTS chỉ cần biết có dòng nào hay không, thường nhanh hơn IN với bảng lớn.",
        ],
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
        theory: [
            "CTE viết bằng WITH ten AS (...) rồi dùng ten như một bảng tạm trong câu truy vấn chính.",
            "Nhiều CTE nối nhau bằng dấu phẩy, CTE sau dùng được kết quả của CTE trước.",
            "CTE giúp tách bài toán thành từng bước, dễ đọc và dễ kiểm tra hơn subquery lồng nhiều tầng.",
            "CTE đệ quy (WITH RECURSIVE) duyệt dữ liệu dạng cây như sơ đồ tổ chức hay danh mục nhiều cấp.",
        ],
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
        theory: [
            "Window function tính trên một khung dòng liên quan nhưng vẫn giữ nguyên số dòng, khác với GROUP BY.",
            "OVER (PARTITION BY ...) chia dữ liệu thành từng nhóm, ORDER BY trong OVER quy định thứ tự trong nhóm.",
            "ROW_NUMBER đánh số liên tục; RANK và DENSE_RANK xử lý giá trị bằng nhau theo hai cách khác nhau.",
            "LAG và LEAD lấy giá trị của dòng trước, dòng sau; SUM() OVER dùng để cộng dồn.",
        ],
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
