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

export const VERDICT_TEXT_CLASSES: Record<Verdict, string> = {
    AC: "text-verdict-ac",
    WA: "text-verdict-wa",
    OM: "text-verdict-om",
    TLE: "text-verdict-tle",
    RE: "text-verdict-re",
    FS: "text-verdict-fs",
    SE: "text-muted-foreground",
};

export const VERDICT_FILL_CLASSES: Record<Verdict, string> = {
    AC: "bg-verdict-ac",
    WA: "bg-verdict-wa",
    OM: "bg-verdict-om",
    TLE: "bg-verdict-tle",
    RE: "bg-verdict-re",
    FS: "bg-verdict-fs",
    SE: "bg-muted-foreground",
};

export const VERDICT_STROKE_CLASSES: Record<Verdict, string> = {
    AC: "stroke-verdict-ac",
    WA: "stroke-verdict-wa",
    OM: "stroke-verdict-om",
    TLE: "stroke-verdict-tle",
    RE: "stroke-verdict-re",
    FS: "stroke-verdict-fs",
    SE: "stroke-muted-foreground",
};
