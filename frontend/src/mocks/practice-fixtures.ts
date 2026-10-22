import type { ProblemProgress, ProblemSummary, Topic } from "@/features/practice/types";
import type { Difficulty } from "@/lib/difficulty";

export const PRACTICE_TOPICS: Topic[] = [
    { slug: "select-where", name: "SELECT/WHERE" },
    { slug: "join", name: "JOIN" },
    { slug: "group-by", name: "GROUP BY/HAVING" },
    { slug: "subquery", name: "Subquery" },
    { slug: "cte", name: "CTE" },
    { slug: "window-function", name: "Window Function" },
    { slug: "null", name: "NULL" },
    { slug: "date-time", name: "Ngày giờ" },
];

type ProblemSeed = [
    number: number,
    slug: string,
    title: string,
    difficulty: Difficulty,
    topicSlugs: string[],
    acceptanceRate: number,
    solvedCount: number,
];

const PROBLEM_SEEDS: ProblemSeed[] = [
    [
        8,
        "count-orders-by-status",
        "Đếm đơn hàng theo trạng thái",
        "EASY",
        ["group-by", "null"],
        62.5,
        21480,
    ],
    [
        12,
        "customers-who-never-order",
        "Khách hàng chưa từng đặt hàng",
        "EASY",
        ["join", "null"],
        68.4,
        24112,
    ],
    [18, "employees-and-manager-names", "Nhân viên và tên quản lý", "EASY", ["join"], 61, 19870],
    [27, "unsold-products", "Sản phẩm chưa bán được", "EASY", ["join", "null"], 57.3, 17402],
    [
        31,
        "employees-without-manager",
        "Nhân viên không có quản lý",
        "EASY",
        ["select-where", "null"],
        71.3,
        25930,
    ],
    [
        34,
        "orders-with-customer-and-product",
        "Đơn hàng kèm tên khách và sản phẩm",
        "EASY",
        ["join"],
        55.8,
        16995,
    ],
    [41, "classmate-pairs", "Cặp sinh viên cùng lớp", "MEDIUM", ["join"], 44.1, 9430],
    [
        52,
        "revenue-by-category",
        "Doanh thu theo danh mục",
        "MEDIUM",
        ["join", "group-by"],
        48.7,
        11204,
    ],
    [66, "connecting-flights", "Chuyến bay nối chuyến", "MEDIUM", ["join"], 36.9, 6118],
    [
        73,
        "customers-who-bought-all-products",
        "Khách hàng mua đủ mọi sản phẩm",
        "HARD",
        ["join", "group-by"],
        23.6,
        3120,
    ],
    [
        77,
        "top-three-products-per-month",
        "Top 3 sản phẩm bán chạy mỗi tháng",
        "HARD",
        ["window-function", "cte", "date-time"],
        21.5,
        2884,
    ],
    [
        79,
        "departments-without-employees",
        "Phòng ban không có nhân viên",
        "EASY",
        ["join", "null"],
        59.2,
        14530,
    ],
    [
        95,
        "students-and-enrolled-courses",
        "Học viên và khoá học đã đăng ký",
        "EASY",
        ["join"],
        63.8,
        13007,
    ],
    [102, "friends-of-friends", "Bạn của bạn", "MEDIUM", ["join"], 33.4, 5276],
    [
        117,
        "booking-gaps",
        "Khoảng trống trong lịch đặt phòng",
        "HARD",
        ["window-function", "date-time"],
        18.2,
        1940,
    ],
    [
        126,
        "price-at-order-time",
        "Giá sản phẩm tại thời điểm đặt",
        "MEDIUM",
        ["join", "subquery", "date-time"],
        39.6,
        7081,
    ],
    [
        140,
        "compare-sales-of-two-years",
        "So sánh doanh số hai năm",
        "MEDIUM",
        ["cte", "date-time", "null"],
        42,
        8322,
    ],
    [
        152,
        "missing-size-color-combinations",
        "Tổ hợp size và màu còn thiếu",
        "MEDIUM",
        ["join"],
        37.7,
        6004,
    ],
    [168, "customer-referral-chain", "Chuỗi giới thiệu khách hàng", "HARD", ["cte"], 16.4, 1302],
    [
        184,
        "highest-salary-per-department",
        "Lương cao nhất mỗi phòng ban",
        "MEDIUM",
        ["join", "subquery"],
        41.2,
        12380,
    ],
    [
        191,
        "drivers-and-cancelled-trips",
        "Tài xế và chuyến đi bị huỷ",
        "MEDIUM",
        ["join", "null"],
        35.1,
        5802,
    ],
    [
        205,
        "users-active-on-both-platforms",
        "Người dùng hoạt động cả hai nền tảng",
        "MEDIUM",
        ["join"],
        40.3,
        6993,
    ],
    [
        213,
        "longest-login-streak",
        "Chuỗi ngày đăng nhập liên tiếp dài nhất",
        "HARD",
        ["window-function", "cte", "date-time"],
        15.9,
        1215,
    ],
    [
        219,
        "reconcile-two-transaction-tables",
        "Đối soát hai bảng giao dịch",
        "HARD",
        ["join", "null"],
        19.8,
        1677,
    ],
];

const DEMO_PROGRESS: Record<number, ProblemProgress> = {
    12: "SOLVED",
    18: "SOLVED",
    34: "SOLVED",
    52: "SOLVED",
    95: "SOLVED",
    140: "SOLVED",
    184: "SOLVED",
    27: "ATTEMPTED",
    73: "ATTEMPTED",
    126: "ATTEMPTED",
};

function topicOf(slug: string): Topic {
    const topic = PRACTICE_TOPICS.find((knownTopic) => knownTopic.slug === slug);
    if (!topic) {
        throw new Error(`Unknown topic ${slug}`);
    }
    return topic;
}

export function buildProblemSummaries(isSignedIn: boolean): ProblemSummary[] {
    return PROBLEM_SEEDS.map(
        ([number, slug, title, difficulty, topicSlugs, acceptanceRate, solvedCount]) => ({
            id: `0192f3b0-0000-7000-8000-${number.toString(16).padStart(12, "0")}`,
            number,
            slug,
            title,
            difficulty,
            topics: topicSlugs.map(topicOf),
            acceptanceRate,
            solvedCount,
            progress: isSignedIn ? (DEMO_PROGRESS[number] ?? "NOT_STARTED") : null,
        }),
    );
}
