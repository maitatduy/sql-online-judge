export type Verdict = "AC" | "WA" | "OM" | "TLE" | "RE" | "FS" | "SE";

export const VERDICT_LABELS: Record<Verdict, string> = {
    AC: "Đúng",
    WA: "Sai kết quả",
    OM: "Đúng dữ liệu, sai thứ tự",
    TLE: "Quá thời gian",
    RE: "Lỗi cú pháp hoặc thực thi",
    FS: "Dùng lệnh bị cấm",
    SE: "Lỗi hệ thống chấm",
};
