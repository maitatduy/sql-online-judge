import type { RecentSubmission } from "@/features/profile/types";
import type { Dialect } from "@/lib/dialect";
import type { Verdict } from "@/lib/verdict";

const MINUTE_MS = 60_000;
const MINUTES_PER_DAY = 1_440;

interface SubmissionSeed {
    verdict: Verdict;
    problemNumber: number;
    problemSlug: string;
    problemTitle: string;
    dialect: Dialect;
    minutesAgo: number;
}

const SUBMISSION_SEEDS: SubmissionSeed[] = [
    {
        verdict: "AC",
        problemNumber: 184,
        problemSlug: "highest-salary-per-department",
        problemTitle: "Lương cao nhất mỗi phòng ban",
        dialect: "POSTGRESQL",
        minutesAgo: 5,
    },
    {
        verdict: "AC",
        problemNumber: 12,
        problemSlug: "customers-who-never-order",
        problemTitle: "Khách hàng chưa từng đặt hàng",
        dialect: "SQLITE",
        minutesAgo: 2 * MINUTES_PER_DAY,
    },
    {
        verdict: "AC",
        problemNumber: 95,
        problemSlug: "students-and-enrolled-courses",
        problemTitle: "Học viên và khoá học đã đăng ký",
        dialect: "MYSQL",
        minutesAgo: 3 * MINUTES_PER_DAY,
    },
    {
        verdict: "AC",
        problemNumber: 140,
        problemSlug: "compare-sales-of-two-years",
        problemTitle: "So sánh doanh số hai năm",
        dialect: "POSTGRESQL",
        minutesAgo: 5 * MINUTES_PER_DAY,
    },
    {
        verdict: "AC",
        problemNumber: 52,
        problemSlug: "revenue-by-category",
        problemTitle: "Doanh thu theo danh mục",
        dialect: "POSTGRESQL",
        minutesAgo: 7 * MINUTES_PER_DAY,
    },
    {
        verdict: "WA",
        problemNumber: 52,
        problemSlug: "revenue-by-category",
        problemTitle: "Doanh thu theo danh mục",
        dialect: "POSTGRESQL",
        minutesAgo: 7 * MINUTES_PER_DAY + 30,
    },
    {
        verdict: "TLE",
        problemNumber: 77,
        problemSlug: "top-three-products-per-month",
        problemTitle: "Top 3 sản phẩm bán chạy mỗi tháng",
        dialect: "MYSQL",
        minutesAgo: 9 * MINUTES_PER_DAY,
    },
    {
        verdict: "AC",
        problemNumber: 31,
        problemSlug: "employees-without-manager",
        problemTitle: "Nhân viên không có quản lý",
        dialect: "SQLITE",
        minutesAgo: 12 * MINUTES_PER_DAY,
    },
    {
        verdict: "RE",
        problemNumber: 31,
        problemSlug: "employees-without-manager",
        problemTitle: "Nhân viên không có quản lý",
        dialect: "SQLITE",
        minutesAgo: 12 * MINUTES_PER_DAY + 10,
    },
    {
        verdict: "AC",
        problemNumber: 8,
        problemSlug: "count-orders-by-status",
        problemTitle: "Đếm đơn hàng theo trạng thái",
        dialect: "POSTGRESQL",
        minutesAgo: 15 * MINUTES_PER_DAY,
    },
    {
        verdict: "OM",
        problemNumber: 8,
        problemSlug: "count-orders-by-status",
        problemTitle: "Đếm đơn hàng theo trạng thái",
        dialect: "POSTGRESQL",
        minutesAgo: 15 * MINUTES_PER_DAY + 5,
    },
    {
        verdict: "AC",
        problemNumber: 3,
        problemSlug: "select-all-customers",
        problemTitle: "Chọn tất cả khách hàng",
        dialect: "POSTGRESQL",
        minutesAgo: 21 * MINUTES_PER_DAY,
    },
];

function submissionId(index: number): string {
    return `0192f3a0-0000-7000-8000-00000000a${String(index).padStart(3, "0")}`;
}

export function buildRecentSubmissions(now: Date): RecentSubmission[] {
    return SUBMISSION_SEEDS.map(({ minutesAgo, ...submission }, index) => ({
        ...submission,
        id: submissionId(index),
        submittedAt: new Date(now.getTime() - minutesAgo * MINUTE_MS).toISOString(),
    }));
}
