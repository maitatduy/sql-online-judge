import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    ReportAction,
    ReportCase,
    ReportCaseDetail,
    ReportStatus,
    ReportTarget,
} from "@/features/admin/reports/types";

import { apiError } from "./api-error";
import { readMockSession } from "./session";

interface MockReportCase {
    detail: ReportCaseDetail;
    status: ReportStatus;
}

let reportCases: MockReportCase[] = [];

function mockCase(
    targetType: ReportTarget,
    index: number,
    content: ReportCaseDetail["content"],
    authorUsername: string,
): MockReportCase {
    const createdAt = new Date(Date.UTC(2026, 10, 23, 8 + index)).toISOString();
    return {
        status: "OPEN",
        detail: {
            targetType,
            targetId: `0192f3a0-0000-7000-a000-00000000000${index}`,
            authorId: `0192f3a0-0000-7000-b000-00000000000${index}`,
            authorUsername,
            priorViolationCount: index - 1,
            content,
            reports: Array.from({ length: 4 - index }, (_, reportIndex) => ({
                id: `0192f3a0-0000-7000-c000-0000000000${index}${reportIndex}`,
                reporterId: `0192f3a0-0000-7000-d000-0000000000${index}${reportIndex}`,
                reporterUsername: `nguoi.bao.cao.${reportIndex + 1}`,
                reason: reportIndex === 0 ? "CONTEST_SPOILER" : "SPAM",
                detail: reportIndex === 0 ? "Đăng lời giải bài B khi contest đang chạy" : null,
                status: "OPEN",
                createdAt,
            })),
        },
    };
}

export function resetAdminReports() {
    reportCases = [
        mockCase(
            "COMMENT",
            1,
            {
                title: null,
                body: "Đáp án bài B: `SELECT dept, MAX(salary) FROM employees GROUP BY dept`",
                postId: "0192f3a0-0000-7000-9000-000000000001",
                problemId: null,
                isHidden: true,
                isDeleted: false,
            },
            "khoa.tran",
        ),
        mockCase(
            "POST",
            2,
            {
                title: "Mua khoá học SQL giá rẻ",
                body: "Liên hệ để mua khoá học.",
                postId: "0192f3a0-0000-7000-9000-000000000002",
                problemId: null,
                isHidden: false,
                isDeleted: false,
            },
            "spam.bot",
        ),
    ];
}

resetAdminReports();

function summaryOf({ detail }: MockReportCase): ReportCase {
    const reasons: ReportCase["reasons"] = {};
    detail.reports.forEach((report) => {
        reasons[report.reason] = (reasons[report.reason] ?? 0) + 1;
    });
    return {
        targetType: detail.targetType,
        targetId: detail.targetId,
        authorId: detail.authorId,
        authorUsername: detail.authorUsername,
        preview: detail.content.title ?? detail.content.body ?? "",
        reportCount: detail.reports.length,
        reasons,
        latestReportAt: detail.reports[0]?.createdAt ?? new Date().toISOString(),
    };
}

function canModerate(): boolean {
    const role = readMockSession()?.role;
    return role === "ADMIN" || role === "MODERATOR";
}

function findCase(targetType: unknown, targetId: unknown): MockReportCase | undefined {
    return reportCases.find(
        ({ detail }) => detail.targetType === targetType && detail.targetId === targetId,
    );
}

export const adminReportHandlers: RequestHandler[] = [
    http.get("*/api/admin/reports", async ({ request }) => {
        await delay(100);
        if (!canModerate()) {
            return apiError(403, "ACCESS_DENIED", "Bạn không có quyền");
        }
        const status = new URL(request.url).searchParams.get("status") ?? "OPEN";
        const items = reportCases.filter((reportCase) => reportCase.status === status);
        return HttpResponse.json({
            items: items.map(summaryOf),
            page: 1,
            size: 50,
            totalItems: items.length,
            totalPages: 1,
        });
    }),
    http.get("*/api/admin/reports/:targetType/:targetId", async ({ params }) => {
        await delay(100);
        const reportCase = findCase(params.targetType, params.targetId);
        if (!canModerate() || !reportCase) {
            return apiError(404, "REPORT_NOT_FOUND", "Không có báo cáo");
        }
        return HttpResponse.json(reportCase.detail);
    }),
    http.post("*/api/admin/reports/:targetType/:targetId/resolve", async ({ params, request }) => {
        const reportCase = findCase(params.targetType, params.targetId);
        if (!canModerate() || !reportCase || reportCase.status !== "OPEN") {
            return apiError(404, "REPORT_NOT_FOUND", "Không có báo cáo nào đang chờ xử lý");
        }
        const { action } = (await request.json()) as { action: ReportAction };
        reportCase.status = action === "REMOVE" ? "RESOLVED" : "DISMISSED";
        reportCase.detail.content.isHidden = action === "REMOVE";
        return new HttpResponse(null, { status: 204 });
    }),
];
